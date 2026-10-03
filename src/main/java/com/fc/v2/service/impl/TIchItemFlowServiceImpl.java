package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TIchItemFlowMapper;
import com.fc.v2.mapper.auto.TIchItemFlowRecordMapper;
import com.fc.v2.mapper.auto.TIchItemFlowVersionMapper;
import com.fc.v2.mapper.auto.TIchProjectMapper;
import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.auto.TIchItemFlowRecord;
import com.fc.v2.model.auto.TIchItemFlowVersion;
import com.fc.v2.model.auto.TIchProject;
import com.fc.v2.model.custom.itemflow.ItemFlowException;
import com.fc.v2.model.custom.itemflow.ItemFlowForm;
import com.fc.v2.model.custom.itemflow.ItemFlowRuler;
import com.fc.v2.model.custom.itemflow.ItemFlowView;
import com.fc.v2.model.custom.itemflow.ItemVersionView;
import com.fc.v2.model.custom.itemflow.RosterView;
import com.fc.v2.model.custom.itemflow.StageRecordView;
import com.fc.v2.service.ITIchItemFlowService;

/**
 * 名录项目申报单 Service业务层处理（state-machine 形状：四格流转）。
 *
 * <p>一份申报归在第几格、后一格收不收它，这两问只在 {@link #pushForward} 这一个推进方法里定。
 * 身处第几格靠顺着留痕表点已过之格得出（{@link ItemFlowRuler#currentStage}），
 * 纸面上的 stage 与接口递上的格号都只算意图，页面不留第二处能挪格次的把手。
 * 老查格次方法 {@link #selectTIchItemFlowById} 签名原样保留、不收新参；
 * {@link #advance}/{@link #rollback} 只作旧签名保留，事事仍回这一个口子。
 *
 * <p>收口（列入/注销/终止）当场锁档：卷面改不动、整张删不掉；
 * 同格第二遍不另起一行；一次只收一格；变更旧版挪去往期、名录只露最新版——
 * 四样一律以推进方法给的回话为准。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIchItemFlowServiceImpl implements ITIchItemFlowService {

    /** 申领会落 */
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TIchItemFlowMapper ichItemFlowMapper;

    @javax.annotation.Resource
    private TIchItemFlowRecordMapper ichItemFlowRecordMapper;

    @javax.annotation.Resource
    private TIchItemFlowVersionMapper ichItemFlowVersionMapper;

    @javax.annotation.Resource
    private TIchProjectMapper ichProjectMapper;

    @Override
    public TIchItemFlow selectTIchItemFlowById(Long id) {
        return this.ichItemFlowMapper.selectById(id);
    }

    @Override
    public List<TIchItemFlow> selectTIchItemFlowList(QueryWrapper<TIchItemFlow> queryWrapper) {
        return this.ichItemFlowMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemFlowView open(ItemFlowForm form) {
        ItemFlowForm data = form == null ? new ItemFlowForm() : form;
        if (!ItemFlowRuler.isValidCategory(data.getCategory())) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_CATEGORY,
                    "门类不在民间文学/传统技艺/传统医药/传统音乐四家之内，立不住单");
        }
        String declareNo = trim(data.getDeclareNo());
        if (!declareNo.isEmpty() && hasRunning(declareNo, null)) {
            throw new ItemFlowException(ItemFlowException.Reason.DUPLICATE_RUNNING,
                    "申报 " + declareNo + " 已有一张在跑的单，头一张没办完也没喊停，后一张立不住");
        }

        Date now = new Date();
        TIchItemFlow bill = new TIchItemFlow();
        bill.setDeclareNo(declareNo.isEmpty() ? null : declareNo);
        bill.setItemName(trimToNull(data.getItemName()));
        bill.setCategory(data.getCategory().trim());
        bill.setApplyArea(trimToNull(data.getApplyArea()));
        bill.setProtectUnit(trimToNull(data.getProtectUnit()));
        bill.setPublicDays(data.getPublicDays());
        bill.setPublicStart(data.getPublicStart());
        bill.setExpertOk(data.getExpertOk());
        bill.setMeetingOk(data.getMeetingOk());
        bill.setStage(ItemFlowRuler.STAGE_VERIFY);
        bill.setStatus(STATUS_IDLE);
        bill.setCloseOutcome(null);
        bill.setCurrentVersion(1);
        bill.setListedFlag(0);
        bill.setContent(trimToNull(data.getRecordText()));
        bill.setLastAction("立单");
        bill.setDelFlag(0);
        bill.setCreateTime(now);
        this.ichItemFlowMapper.insert(bill);
        if (trim(data.getBizNo()).isEmpty()) {
            bill.setBizNo("SB" + bill.getId());
        } else {
            bill.setBizNo(data.getBizNo().trim());
        }
        this.ichItemFlowMapper.updateById(bill);
        return view(bill.getId(), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemFlowView report(Long id, ItemFlowForm form) {
        TIchItemFlow bill = requireBill(id);
        requireNotClosed(bill);
        ItemFlowForm data = form == null ? new ItemFlowForm() : form;
        if (data.getCategory() != null && !ItemFlowRuler.isValidCategory(data.getCategory())) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_CATEGORY,
                    "门类不在民间文学/传统技艺/传统医药/传统音乐四家之内");
        }

        // 材料并到单据：本格收不收，推进时由尺照最新材料点
        mergeMaterials(bill, data);

        List<TIchItemFlowRecord> records = loadRecords(id);
        int current = ItemFlowRuler.currentStage(records);
        TIchItemFlowRecord pending = ItemFlowRuler.pendingRecord(records, current);
        if (pending == null) {
            // 头一遍上报：另起一笔，卷面压在这一格
            TIchItemFlowRecord row = new TIchItemFlowRecord();
            row.setBillId(id);
            row.setStage(current);
            row.setRoundNo(ItemFlowRuler.nextRoundOf(records, current));
            row.setRecordText(trimToNull(data.getRecordText()));
            row.setPassFlag(0);
            row.setPressed(0);
            row.setEnterTime(new Date());
            row.setDelFlag(0);
            this.ichItemFlowRecordMapper.insert(row);
        } else {
            // 同一格第二次上报：不另起一行，卷面仍留头一遍那句，一个字都不覆盖
        }

        // 卷面那句随头一遍上报落定
        if (trim(bill.getContent()).isEmpty() && !trim(data.getRecordText()).isEmpty()) {
            bill.setContent(data.getRecordText().trim());
        }
        bill.setStatus(STATUS_ACTIVE);
        bill.setLastAction("上报" + ItemFlowRuler.stageName(current));
        this.ichItemFlowMapper.updateById(bill);
        return view(id, new Date());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemFlowView pushForward(Long id, Integer intendedStage, Date at) {
        Date now = at == null ? new Date() : at;
        TIchItemFlow bill = requireBill(id);
        requireNotClosed(bill);

        List<TIchItemFlowRecord> records = loadRecords(id);
        int current = ItemFlowRuler.currentStage(records);

        // 门槛先过：本格没过，后一格开着也不能进
        ItemFlowRuler.Gate gate = ItemFlowRuler.gateOf(current, bill, now);
        if (!gate.isPassed()) {
            throw new ItemFlowException(ItemFlowException.Reason.GATE_NOT_MET,
                    ItemFlowRuler.stageName(current) + "门槛没过：" + String.join("；", gate.getMissing()));
        }

        boolean listing = current == ItemFlowRuler.LAST_STAGE;
        int target = listing ? ItemFlowRuler.LAST_STAGE : current + 1;
        // 纸面带出的格号与回算相左（含一次想收两格、越格直推）：一概按回算的来
        if (intendedStage != null && intendedStage.intValue() != target) {
            throw new ItemFlowException(ItemFlowException.Reason.STAGE_SKIPPED,
                    "意向格 " + intendedStage + " 与挨着的下一格 " + target + " 相左，前进只到挨着的那一格");
        }

        // 本格头一遍那笔翻成已过口；没先报过就点推进的，补一笔已过口留痕（仍只此一笔）
        TIchItemFlowRecord rec = ItemFlowRuler.pendingRecord(records, current);
        if (rec == null) {
            rec = new TIchItemFlowRecord();
            rec.setBillId(id);
            rec.setStage(current);
            rec.setRoundNo(ItemFlowRuler.nextRoundOf(records, current));
            rec.setPassFlag(1);
            rec.setPressed(0);
            rec.setEnterTime(now);
            rec.setDelFlag(0);
            this.ichItemFlowRecordMapper.insert(rec);
        } else {
            TIchItemFlowRecord flip = new TIchItemFlowRecord();
            flip.setId(rec.getId());
            flip.setPassFlag(1);
            this.ichItemFlowRecordMapper.updateById(flip);
        }

        if (listing) {
            doList(bill, now);
        } else {
            TIchItemFlow patch = new TIchItemFlow();
            patch.setStage(target);
            patch.setStatus(STATUS_ACTIVE);
            patch.setLastAction("过" + ItemFlowRuler.stageName(current));
            patch.setUpdateTime(now);
            compareAndUpdateStage(bill, current, patch);
            bill.setStage(target);
            bill.setStatus(STATUS_ACTIVE);
        }
        return view(id, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemFlowView pullBack(Long id, Date at) {
        Date now = at == null ? new Date() : at;
        TIchItemFlow bill = requireBill(id);
        requireNotClosed(bill);

        List<TIchItemFlowRecord> records = loadRecords(id);
        int current = ItemFlowRuler.currentStage(records);
        if (current == ItemFlowRuler.STAGE_VERIFY) {
            throw new ItemFlowException(ItemFlowException.Reason.NO_PREV_STAGE,
                    "已在头一格（形式核验），再无紧挨的上一格可退");
        }
        int prev = current - 1;

        // 退回后这一段先前所记一并压到下面：紧挨上一格那笔过口、以及离开那一格已在卷面
        // 攒着的草稿，都算旧账。旧笔一笔不抹；重走从退回格再从头攒（另起一笔、轮次加一）。
        for (TIchItemFlowRecord r : records) {
            if (r.getStage() != null && r.getStage() >= prev
                    && (r.getPressed() == null || r.getPressed() == 0)) {
                TIchItemFlowRecord press = new TIchItemFlowRecord();
                press.setId(r.getId());
                press.setPressed(1);
                this.ichItemFlowRecordMapper.updateById(press);
            }
        }

        // 退到核验头一格即重回"未起"（该格旧记已压下，从头攒）；退到其余格仍"在办"
        int status = prev == ItemFlowRuler.STAGE_VERIFY ? STATUS_IDLE : STATUS_ACTIVE;
        TIchItemFlow patch = new TIchItemFlow();
        patch.setStage(prev);
        patch.setStatus(status);
        patch.setLastAction("退回" + ItemFlowRuler.stageName(prev));
        patch.setUpdateTime(now);
        compareAndUpdateStage(bill, current, patch);
        return view(id, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemFlowView close(Long id, int outcome, String reason, Date at) {
        Date now = at == null ? new Date() : at;
        TIchItemFlow bill = requireBill(id);
        if (outcome != ItemFlowRuler.CLOSE_CANCELLED && outcome != ItemFlowRuler.CLOSE_TERMINATED) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_CLOSE_OUTCOME,
                    "收口只认注销/终止两说；列入由列入格认下，不许嘴上点名");
        }

        boolean alreadyClosed = bill.getStatus() != null && bill.getStatus() == STATUS_TERMINAL;
        boolean wasListed = bill.getCloseOutcome() != null
                && bill.getCloseOutcome() == ItemFlowRuler.CLOSE_LISTED;
        if (alreadyClosed) {
            // 已列入的还能再走"注销"这一说：在册项目随注销减一。列入事实、格次、校验码一笔不抹，
            // 只翻收口说法、挪开现行名录、记下注销缘由与那一刻——年末凭它追凭什么列、凭什么注销。
            // 注销/终止之后或对已列入单点"终止"，一律锁死。
            if (!(wasListed && outcome == ItemFlowRuler.CLOSE_CANCELLED)) {
                throw new ItemFlowException(ItemFlowException.Reason.LOCKED,
                        "单据已收口（" + ItemFlowRuler.closeOutcomeName(bill.getCloseOutcome())
                                + "）当场锁档，不能再换说法");
            }
            delist(bill, reason, now);
            return view(id, now);
        }

        TIchItemFlow patch = new TIchItemFlow();
        patch.setStatus(STATUS_TERMINAL);
        patch.setCloseOutcome(outcome);
        patch.setLastAction(ItemFlowRuler.closeOutcomeName(outcome) + "收口");
        patch.setRemark(trimToNull(reason));
        patch.setUpdateTime(now);
        UpdateWrapper<TIchItemFlow> uw = new UpdateWrapper<>();
        uw.eq("id", id).ne("status", STATUS_TERMINAL);
        if (this.ichItemFlowMapper.update(patch, uw) == 0) {
            throw new ItemFlowException(ItemFlowException.Reason.CONCURRENT_CHANGED,
                    "单据刚被别人收口，请按最新情形重新办理");
        }
        bill.setStatus(STATUS_TERMINAL);
        bill.setCloseOutcome(outcome);
        return view(id, now);
    }

    /** 已列入项目再注销：翻说法、挪开现行名录、底册销号；列入档案与校验码原样留底 */
    private void delist(TIchItemFlow bill, String reason, Date now) {
        TIchItemFlow patch = new TIchItemFlow();
        patch.setId(bill.getId());
        patch.setCloseOutcome(ItemFlowRuler.CLOSE_CANCELLED);
        patch.setListedFlag(0);
        patch.setCancelReason(trimToNull(reason));
        patch.setCancelTime(now);
        patch.setLastAction("列入后注销");
        patch.setUpdateTime(now);
        UpdateWrapper<TIchItemFlow> uw = new UpdateWrapper<>();
        uw.eq("id", bill.getId())
                .eq("close_outcome", ItemFlowRuler.CLOSE_LISTED);
        if (this.ichItemFlowMapper.update(patch, uw) == 0) {
            throw new ItemFlowException(ItemFlowException.Reason.CONCURRENT_CHANGED,
                    "注销落笔瞬间说法已被别人改动，请按最新情形重新办理");
        }
        if (bill.getProjectId() != null) {
            TIchProject p = new TIchProject();
            p.setId(bill.getProjectId());
            p.setStatus(1);
            p.setUpdateTime(now);
            this.ichProjectMapper.updateById(p);
        }
        TIchItemFlowVersion offCurrent = new TIchItemFlowVersion();
        offCurrent.setCurrentFlag(0);
        this.ichItemFlowVersionMapper.update(offCurrent,
                new UpdateWrapper<TIchItemFlowVersion>()
                        .eq("bill_id", bill.getId()).eq("current_flag", 1));

        bill.setCloseOutcome(ItemFlowRuler.CLOSE_CANCELLED);
        bill.setListedFlag(0);
        bill.setCancelReason(trimToNull(reason));
        bill.setCancelTime(now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemVersionView revise(Long id, ItemFlowForm form, Date at) {
        Date now = at == null ? new Date() : at;
        TIchItemFlow bill = requireBill(id);
        // 变更只接在已列入、现行名录上露着的单
        if (bill.getCloseOutcome() == null || bill.getCloseOutcome() != ItemFlowRuler.CLOSE_LISTED
                || bill.getListedFlag() == null || bill.getListedFlag() != 1) {
            throw new ItemFlowException(ItemFlowException.Reason.NOT_LISTED,
                    "只有已列入现行名录的单才谈得上变更；未列入或已注销/终止的不另起版");
        }
        ItemFlowForm data = form == null ? new ItemFlowForm() : form;
        String newName = data.getItemName() == null ? bill.getItemName() : data.getItemName().trim();
        String newUnit = data.getProtectUnit() == null ? bill.getProtectUnit() : data.getProtectUnit().trim();
        String newCategory = data.getCategory() == null ? bill.getCategory() : data.getCategory().trim();
        if (data.getCategory() != null && !ItemFlowRuler.isValidCategory(newCategory)) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_CATEGORY,
                    "门类不在民间文学/传统技艺/传统医药/传统音乐四家之内");
        }
        boolean nameChanged = !eq(newName, bill.getItemName());
        boolean unitChanged = !eq(newUnit, bill.getProtectUnit());
        if (!nameChanged && !unitChanged) {
            throw new ItemFlowException(ItemFlowException.Reason.NOTHING_CHANGED,
                    "名称与保护单位原样照抄，没有一样真变了，不另起一版");
        }

        int oldVersion = bill.getCurrentVersion() == null ? 1 : bill.getCurrentVersion();
        int newVersion = oldVersion + 1;
        String oldCode = bill.getEntryCode();

        // 旧版从现行名录挪开：旧版次行翻成往期，一笔不删，留给人翻
        TIchItemFlowVersion retire = new TIchItemFlowVersion();
        retire.setCurrentFlag(0);
        retire.setUpdateTime(now);
        this.ichItemFlowVersionMapper.update(retire, new UpdateWrapper<TIchItemFlowVersion>()
                .eq("bill_id", id).eq("current_flag", 1));

        // 新版重算校验码：版次在码里，新旧显成同一个码便是错的
        String newCode = ItemFlowRuler.entryCode(bill.getBizNo(), bill.getDeclareNo(), newVersion,
                newName, newCategory, bill.getApplyArea(), newUnit, now);
        if (ItemFlowRuler.versionsConflict(oldCode, newCode)) {
            // 纯算上不该到这里；到了即同料两码撞了，宁可不落
            throw new ItemFlowException(ItemFlowException.Reason.CONCURRENT_CHANGED,
                    "新版校验码与旧版撞了同一串码，按错论，暂不落版");
        }

        TIchItemFlowVersion row = new TIchItemFlowVersion();
        row.setBillId(id);
        row.setVersionNo(newVersion);
        row.setItemName(newName);
        row.setCategory(newCategory);
        row.setApplyArea(bill.getApplyArea());
        row.setProtectUnit(newUnit);
        row.setEntryCode(newCode);
        row.setCurrentFlag(1);
        row.setListedTime(now);
        row.setDelFlag(0);
        this.ichItemFlowVersionMapper.insert(row);

        TIchItemFlow patch = new TIchItemFlow();
        patch.setId(id);
        patch.setItemName(newName);
        patch.setProtectUnit(newUnit);
        patch.setCategory(newCategory);
        patch.setCurrentVersion(newVersion);
        patch.setListedFlag(1);
        patch.setEntryCode(newCode);
        patch.setLastAction("变更第" + newVersion + "版");
        patch.setUpdateTime(now);
        this.ichItemFlowMapper.updateById(patch);

        // 底册项目随现行版走：名录上只露最新那一版的名目（在册数不加不减）
        if (bill.getProjectId() != null) {
            TIchProject p = new TIchProject();
            p.setId(bill.getProjectId());
            p.setSiteName(newName);
            p.setSiteType(newCategory);
            p.setUpdateTime(now);
            this.ichProjectMapper.updateById(p);
        }

        bill.setItemName(newName);
        bill.setProtectUnit(newUnit);
        bill.setCategory(newCategory);
        bill.setCurrentVersion(newVersion);
        bill.setEntryCode(newCode);
        return toVersionView(row);
    }

    @Override
    public ItemFlowView view(Long id, Date at) {
        TIchItemFlow bill = requireBill(id);
        return buildView(bill, loadRecords(id), at == null ? new Date() : at);
    }

    @Override
    public List<ItemVersionView> versions(Long id) {
        requireBill(id);
        List<TIchItemFlowVersion> rows = this.ichItemFlowVersionMapper.selectList(
                new QueryWrapper<TIchItemFlowVersion>()
                        .eq("bill_id", id).eq("del_flag", 0)
                        .orderByAsc("version_no"));
        List<ItemVersionView> out = new ArrayList<>();
        for (TIchItemFlowVersion r : rows) {
            out.add(toVersionView(r));
        }
        return out;
    }

    @Override
    public RosterView roster() {
        // 两处数同一回装载点出：一边顺现行版次，一边顺底册在册，不分两回取了再凑
        int byRoster = 0;
        List<TIchItemFlow> bills = this.ichItemFlowMapper.selectList(
                new QueryWrapper<TIchItemFlow>().eq("del_flag", 0));
        for (TIchItemFlow b : bills) {
            if (b.getCloseOutcome() != null && b.getCloseOutcome() == ItemFlowRuler.CLOSE_LISTED
                    && b.getListedFlag() != null && b.getListedFlag() == 1) {
                byRoster++;
            }
        }
        int byLedger = 0;
        List<TIchProject> projects = this.ichProjectMapper.selectList(
                new QueryWrapper<TIchProject>().eq("del_flag", 0));
        for (TIchProject p : projects) {
            if (p.getStatus() != null && p.getStatus() == 0) {
                byLedger++;
            }
        }
        RosterView v = new RosterView();
        v.setByRoster(byRoster);
        v.setByLedger(byLedger);
        v.setConsistent(ItemFlowRuler.rosterConsistent(byRoster, byLedger));
        return v;
    }

    @Override
    public TIchItemFlow advance(Long id, String remark) {
        // 旧签名保留：推进的事仍只走 pushForward 这一个口子，不另设规矩
        try {
            pushForward(id, null, new Date());
        } catch (ItemFlowException e) {
            if (e.getReason() == ItemFlowException.Reason.LOCKED) {
                return this.ichItemFlowMapper.selectById(id);
            }
            return null;
        }
        return this.ichItemFlowMapper.selectById(id);
    }

    @Override
    public TIchItemFlow rollback(Long id, String remark) {
        try {
            pullBack(id, new Date());
        } catch (ItemFlowException e) {
            if (e.getReason() == ItemFlowException.Reason.LOCKED) {
                return this.ichItemFlowMapper.selectById(id);
            }
            return null;
        }
        return this.ichItemFlowMapper.selectById(id);
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TIchItemFlow bill = this.ichItemFlowMapper.selectById(id);
        if (bill == null) {
            return false;
        }
        // 收口锁档：卷面上的字修改不了
        if (bill.getStatus() != null && bill.getStatus() == STATUS_TERMINAL) {
            return false;
        }
        bill.setContent(remark);
        return this.ichItemFlowMapper.updateById(bill) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TIchItemFlow bill = this.ichItemFlowMapper.selectById(id);
        if (bill == null) {
            return false;
        }
        // 收口锁档：整张删不掉
        if (bill.getStatus() != null && bill.getStatus() == STATUS_TERMINAL) {
            return false;
        }
        return this.ichItemFlowMapper.deleteById(id) > 0;
    }

    // ------------------------------------------------------------------

    /** 列入收口：钉校验码、落底册、存头一版、翻现行名录、锁档 */
    private void doList(TIchItemFlow bill, Date now) {
        int version = bill.getCurrentVersion() == null ? 1 : bill.getCurrentVersion();
        String code = ItemFlowRuler.entryCode(bill.getBizNo(), bill.getDeclareNo(), version,
                bill.getItemName(), bill.getCategory(), bill.getApplyArea(), bill.getProtectUnit(), now);

        TIchProject project = new TIchProject();
        project.setSiteNo(firstNonBlank(bill.getBizNo(), bill.getDeclareNo(), "XM" + bill.getId()));
        project.setSiteName(bill.getItemName());
        project.setSiteType(bill.getCategory());
        project.setRoadName(bill.getApplyArea());
        project.setStatus(0);
        project.setDelFlag(0);
        project.setCreateTime(now);
        this.ichProjectMapper.insert(project);

        TIchItemFlowVersion ver = new TIchItemFlowVersion();
        ver.setBillId(bill.getId());
        ver.setVersionNo(version);
        ver.setItemName(bill.getItemName());
        ver.setCategory(bill.getCategory());
        ver.setApplyArea(bill.getApplyArea());
        ver.setProtectUnit(bill.getProtectUnit());
        ver.setEntryCode(code);
        ver.setCurrentFlag(1);
        ver.setListedTime(now);
        ver.setDelFlag(0);
        this.ichItemFlowVersionMapper.insert(ver);

        TIchItemFlow patch = new TIchItemFlow();
        patch.setStage(ItemFlowRuler.LAST_STAGE);
        patch.setStatus(STATUS_TERMINAL);
        patch.setCloseOutcome(ItemFlowRuler.CLOSE_LISTED);
        patch.setListedFlag(1);
        patch.setEntryCode(code);
        patch.setProjectId(project.getId());
        patch.setLastAction("列入名录");
        patch.setUpdateTime(now);
        UpdateWrapper<TIchItemFlow> uw = new UpdateWrapper<>();
        uw.eq("id", bill.getId()).ne("status", STATUS_TERMINAL);
        if (this.ichItemFlowMapper.update(patch, uw) == 0) {
            throw new ItemFlowException(ItemFlowException.Reason.CONCURRENT_CHANGED,
                    "列入落笔瞬间单据已被别人收口，请按最新情形重新办理");
        }
        bill.setStage(ItemFlowRuler.LAST_STAGE);
        bill.setStatus(STATUS_TERMINAL);
        bill.setCloseOutcome(ItemFlowRuler.CLOSE_LISTED);
        bill.setListedFlag(1);
        bill.setEntryCode(code);
        bill.setProjectId(project.getId());
    }

    private void compareAndUpdateStage(TIchItemFlow bill, int expectedStage, TIchItemFlow patch) {
        UpdateWrapper<TIchItemFlow> uw = new UpdateWrapper<>();
        uw.eq("id", bill.getId())
                .eq("stage", expectedStage)
                .ne("status", STATUS_TERMINAL);
        if (this.ichItemFlowMapper.update(patch, uw) == 0) {
            // 落笔瞬间已被别人推动：本次留痕与更新同事务回滚，让调用方按最新回算重试
            throw new ItemFlowException(ItemFlowException.Reason.CONCURRENT_CHANGED,
                    "单据刚被别人推动，请按最新格次重新办理");
        }
    }

    private void mergeMaterials(TIchItemFlow bill, ItemFlowForm data) {
        if (data.getItemName() != null) {
            bill.setItemName(data.getItemName().trim());
        }
        if (data.getCategory() != null) {
            bill.setCategory(data.getCategory().trim());
        }
        if (data.getApplyArea() != null) {
            bill.setApplyArea(data.getApplyArea().trim());
        }
        if (data.getProtectUnit() != null) {
            bill.setProtectUnit(data.getProtectUnit().trim());
        }
        if (data.getPublicDays() != null) {
            bill.setPublicDays(data.getPublicDays());
        }
        if (data.getPublicStart() != null) {
            bill.setPublicStart(data.getPublicStart());
        }
        if (data.getExpertOk() != null) {
            bill.setExpertOk(data.getExpertOk());
        }
        if (data.getMeetingOk() != null) {
            bill.setMeetingOk(data.getMeetingOk());
        }
    }

    private ItemFlowView buildView(TIchItemFlow bill, List<TIchItemFlowRecord> records, Date at) {
        int current = ItemFlowRuler.currentStage(records);
        ItemFlowView v = new ItemFlowView();
        v.setBillId(String.valueOf(bill.getId()));
        v.setBizNo(bill.getBizNo());
        v.setDeclareNo(bill.getDeclareNo());
        v.setCurrentStage(current);
        v.setCurrentStageName(ItemFlowRuler.stageName(current));
        v.setStatus(bill.getStatus() == null ? STATUS_IDLE : bill.getStatus());
        boolean closed = bill.getStatus() != null && bill.getStatus() == STATUS_TERMINAL;
        v.setClosed(closed);
        v.setCloseOutcome(bill.getCloseOutcome());
        v.setCloseOutcomeName(ItemFlowRuler.closeOutcomeName(bill.getCloseOutcome()));
        v.setListed(bill.getCloseOutcome() != null
                && bill.getCloseOutcome() == ItemFlowRuler.CLOSE_LISTED);
        v.setVersionNo(bill.getCurrentVersion() == null ? 1 : bill.getCurrentVersion());
        v.setListedFlag(bill.getListedFlag() != null && bill.getListedFlag() == 1);
        v.setEntryCode(bill.getEntryCode());
        v.setProjectId(bill.getProjectId() == null ? null : String.valueOf(bill.getProjectId()));
        v.setItemName(bill.getItemName());
        v.setCategory(bill.getCategory());
        v.setApplyArea(bill.getApplyArea());
        v.setProtectUnit(bill.getProtectUnit());
        v.setPublicDays(bill.getPublicDays());
        v.setPublicStart(bill.getPublicStart());
        v.setExpertOk(bill.getExpertOk() != null && bill.getExpertOk() == 1);
        v.setMeetingOk(bill.getMeetingOk() != null && bill.getMeetingOk() == 1);

        // 收口后不再点门槛；未收口只点当前这一格，后一格开不开不由这里说了算
        if (!closed) {
            ItemFlowRuler.Gate gate = ItemFlowRuler.gateOf(current, bill, at);
            v.setGatePassed(gate.isPassed());
            v.setGateMissing(gate.getMissing());
        } else {
            v.setGatePassed(true);
            v.setGateMissing(new ArrayList<>());
        }

        List<StageRecordView> rvs = new ArrayList<>();
        for (TIchItemFlowRecord r : ItemFlowRuler.sorted(records)) {
            StageRecordView rv = new StageRecordView();
            rv.setStage(r.getStage() == null ? -1 : r.getStage());
            rv.setStageName(ItemFlowRuler.stageName(rv.getStage()));
            rv.setRoundNo(r.getRoundNo() == null ? 0 : r.getRoundNo());
            rv.setRecordText(r.getRecordText());
            rv.setPressed(r.getPressed() != null && r.getPressed() == 1);
            rv.setEnterTime(r.getEnterTime());
            rvs.add(rv);
        }
        v.setRecords(rvs);

        TIchItemFlowRecord pending = ItemFlowRuler.pendingRecord(records, current);
        v.setContent(pending != null ? pending.getRecordText() : bill.getContent());
        return v;
    }

    private ItemVersionView toVersionView(TIchItemFlowVersion r) {
        ItemVersionView v = new ItemVersionView();
        v.setVersionId(String.valueOf(r.getId()));
        v.setBillId(String.valueOf(r.getBillId()));
        v.setVersionNo(r.getVersionNo() == null ? 1 : r.getVersionNo());
        v.setItemName(r.getItemName());
        v.setCategory(r.getCategory());
        v.setApplyArea(r.getApplyArea());
        v.setProtectUnit(r.getProtectUnit());
        v.setEntryCode(r.getEntryCode());
        v.setCurrent(r.getCurrentFlag() != null && r.getCurrentFlag() == 1);
        v.setListedTime(r.getListedTime());
        return v;
    }

    private TIchItemFlow requireBill(Long id) {
        TIchItemFlow bill = this.ichItemFlowMapper.selectById(id);
        if (bill == null) {
            throw new ItemFlowException(ItemFlowException.Reason.NOT_FOUND, "查无此名录申报单: " + id);
        }
        return bill;
    }

    private void requireNotClosed(TIchItemFlow bill) {
        if (bill.getStatus() != null && bill.getStatus() == STATUS_TERMINAL) {
            throw new ItemFlowException(ItemFlowException.Reason.LOCKED,
                    "单据已收口（" + ItemFlowRuler.closeOutcomeName(bill.getCloseOutcome())
                            + "）当场锁档：卷面上的字改不了，也推不动退不回");
        }
    }

    private boolean hasRunning(String declareNo, Long excludeId) {
        List<TIchItemFlow> same = this.ichItemFlowMapper.selectList(new QueryWrapper<TIchItemFlow>()
                .eq("declare_no", declareNo)
                .eq("del_flag", 0)
                .ne("status", STATUS_TERMINAL));
        for (TIchItemFlow b : same) {
            if (excludeId == null || !excludeId.equals(b.getId())) {
                return true;
            }
        }
        return false;
    }

    private List<TIchItemFlowRecord> loadRecords(Long id) {
        return this.ichItemFlowRecordMapper.selectList(new QueryWrapper<TIchItemFlowRecord>()
                .eq("bill_id", id)
                .eq("del_flag", 0)
                .orderByAsc("enter_time")
                .orderByAsc("id"));
    }

    private static boolean eq(String a, String b) {
        return trim(a).equals(trim(b));
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String... vals) {
        for (String s : vals) {
            if (s != null && !s.trim().isEmpty()) {
                return s.trim();
            }
        }
        return null;
    }
}
