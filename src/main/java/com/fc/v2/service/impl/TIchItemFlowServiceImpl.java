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
import com.fc.v2.mapper.auto.TIchProjectMapper;
import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.auto.TIchItemFlowRecord;
import com.fc.v2.model.auto.TIchProject;
import com.fc.v2.model.custom.itemflow.ItemFlowChain;
import com.fc.v2.model.custom.itemflow.ItemFlowCommand;
import com.fc.v2.model.custom.itemflow.ItemFlowException;
import com.fc.v2.model.custom.itemflow.ItemFlowLedger;
import com.fc.v2.model.custom.itemflow.ItemFlowView;
import com.fc.v2.service.ITIchItemFlowService;

/**
 * 名录项目申报单 Service业务层处理。
 *
 * <p>格次只在 {@link #move} 这一个推进口里挪：前进只到紧挨的下一格、后退只退一格；归在第几格
 * 顺着 {@link ItemFlowChain} 的卷面留痕回算，纸上与接口递来的格号都只当意向。收口当场锁档，
 * 卷面的字改不动、整张删不掉；同格第二遍不另起一行；一份申报只容一张在跑；变更另起一版，
 * 旧版转往期、校验码重算；在册数列入 +1、注销 −1，与底册同一回算对得齐。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIchItemFlowServiceImpl implements ITIchItemFlowService {

    @javax.annotation.Resource
    private TIchItemFlowMapper ichItemFlowMapper;

    @javax.annotation.Resource
    private TIchItemFlowRecordMapper ichItemFlowRecordMapper;

    @javax.annotation.Resource
    private TIchProjectMapper ichProjectMapper;

    @Override
    public TIchItemFlow selectTIchItemFlowById(Long id) {
        // 老的取格次方法签名原样保留：收什么给什么，不回算、不塞新参、不推进
        return this.ichItemFlowMapper.selectById(id);
    }

    @Override
    public List<TIchItemFlow> selectTIchItemFlowList(QueryWrapper<TIchItemFlow> queryWrapper) {
        return this.ichItemFlowMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemFlowView declare(ItemFlowCommand command) {
        if (command == null) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_ARGUMENT, "开单入参不能为空");
        }
        String declareNo = trim(command.getDeclareNo());
        if (declareNo.isEmpty()) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_ARGUMENT, "申报归口号不能为空");
        }
        if (blank(command.getItemName()) || blank(command.getApplyArea())
                || blank(command.getProtectUnit())) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_ARGUMENT,
                    "形式核验四样要齐：项目名称、门类、申报地、保护单位");
        }
        if (!ItemFlowChain.validCategory(command.getSiteType())) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_CATEGORY,
                    "门类只收民间文学/传统技艺/传统医药/传统音乐");
        }

        // 同一份申报只容一张在跑的单：头一张没办完、也没喊停之前，后一张立不住
        Integer running = ichItemFlowMapper.selectCount(new QueryWrapper<TIchItemFlow>()
                .eq("declare_no", declareNo)
                .eq("del_flag", 0)
                .ne("status", ItemFlowChain.STATUS_CLOSED));
        if (running != null && running > 0) {
            throw new ItemFlowException(ItemFlowException.Reason.ALREADY_RUNNING,
                    "申报 " + declareNo + " 已有一张在跑的单，未收口前不能再立一张");
        }

        TIchItemFlow flow = new TIchItemFlow();
        flow.setBizNo(blank(command.getBizNo()) ? declareNo : trim(command.getBizNo()));
        flow.setDeclareNo(declareNo);
        flow.setVersionNo(0);
        flow.setCurrentFlag(ItemFlowChain.CURRENT_FLAG_YES);
        flow.setRoundNo(0);
        flow.setStage(0);
        flow.setStatus(ItemFlowChain.STATUS_IDLE);
        flow.setCloseType(ItemFlowChain.CLOSE_NONE);
        flow.setItemName(trim(command.getItemName()));
        flow.setSiteType(trim(command.getSiteType()));
        flow.setApplyArea(trim(command.getApplyArea()));
        flow.setProtectUnit(trim(command.getProtectUnit()));
        flow.setPublicDays(command.getPublicDays());
        flow.setDelFlag(0);
        this.ichItemFlowMapper.insert(flow);
        return ItemFlowChain.buildView(flow, new ArrayList<>());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemFlowView move(Long id, boolean advance, Integer intendedStage, String note, Date now) {
        TIchItemFlow flow = requireFlow(id);
        if (isLocked(flow)) {
            throw new ItemFlowException(ItemFlowException.Reason.LOCKED,
                    "申报单已收口当场锁档，格次挪不动");
        }
        List<TIchItemFlowRecord> records = loadRecords(id);
        ItemFlowChain.State before = ItemFlowChain.evaluate(flow, records);
        int current = before.getCurrentStage();

        // 屏上带出的格次与推进方法回的不一致时，听推进方法的——意向格号只算意图
        if (intendedStage != null && intendedStage.intValue() != current) {
            throw new ItemFlowException(ItemFlowException.Reason.STAGE_MISMATCH,
                    "意向格次 " + intendedStage + " 与入口点出的当前格 " + current
                            + " 相左，按入口的来");
        }
        Date at = now == null ? new Date() : now;

        if (advance) {
            return doAdvance(flow, records, before, current, note, at);
        }
        return doBack(flow, records, before, current, note, at);
    }

    private ItemFlowView doAdvance(TIchItemFlow flow, List<TIchItemFlowRecord> records,
                                   ItemFlowChain.State before, int current, String note, Date at) {
        int round = before.getCurrentRound();

        // 同格第二遍上报：头一遍那句已在卷面上，不再另起一行（重复提交原样回，不写第二笔）
        if (hasAdvance(records, current, round)) {
            return ItemFlowChain.buildView(flow, records);
        }

        // 后一格收不收只看当前格门槛过没过：日子没走完，材料再齐、后一格开着也不能进
        ItemFlowChain.Gate gate = ItemFlowChain.gateOf(flow, current, at);
        if (!gate.isPassed()) {
            throw new ItemFlowException(ItemFlowException.Reason.GATE_NOT_MET,
                    ItemFlowChain.stageName(current) + " 门槛没过：" + gate.getReason());
        }

        boolean listing = current == ItemFlowChain.LAST_STAGE;

        TIchItemFlowRecord rec = new TIchItemFlowRecord();
        rec.setFlowId(flow.getId());
        rec.setStage(current);
        rec.setRoundNo(round);
        rec.setAction(ItemFlowChain.ACTION_ADVANCE);
        rec.setNote(note);
        rec.setActionTime(at);
        rec.setDelFlag(0);
        this.ichItemFlowRecordMapper.insert(rec);
        records.add(rec);

        TIchItemFlow patch = new TIchItemFlow();
        UpdateWrapper<TIchItemFlow> uw = new UpdateWrapper<>();
        uw.eq("id", flow.getId())
                .eq("stage", current)
                .eq("round_no", round)
                .ne("status", ItemFlowChain.STATUS_CLOSED);

        int nextStage;
        if (listing) {
            // 走到列入：校验码此刻算定钉死，项目入册（在册数 +1）
            String code = ItemFlowChain.checkCode(flow);
            assertCodeUnique(flow.getDeclareNo(), code, flow.getId());
            String siteNo = ensureProjectActive(flow);
            patch.setStatus(ItemFlowChain.STATUS_CLOSED);
            patch.setCloseType(ItemFlowChain.CLOSE_LISTED);
            patch.setCheckCode(code);
            patch.setListedTime(at);
            patch.setSiteNo(siteNo);
            nextStage = ItemFlowChain.LAST_STAGE;
        } else {
            patch.setStatus(ItemFlowChain.STATUS_RUNNING);
            nextStage = current + 1;
            // 进社会公示格起算公示日子
            if (nextStage == 2) {
                patch.setPublicStart(flow.getPublicStart() == null ? at : flow.getPublicStart());
            }
        }
        patch.setStage(nextStage);
        patch.setLastAction(ItemFlowChain.ACTION_ADVANCE);
        patch.setUpdateTime(at);

        if (this.ichItemFlowMapper.update(patch, uw) == 0) {
            throw new ItemFlowException(ItemFlowException.Reason.CONCURRENT_CHANGED,
                    "申报单刚被别人推动，请按最新格次重新办理");
        }
        mirror(flow, patch);
        return ItemFlowChain.buildView(flow, records);
    }

    private ItemFlowView doBack(TIchItemFlow flow, List<TIchItemFlowRecord> records,
                                ItemFlowChain.State before, int current, String note, Date at) {
        if (current == 0) {
            throw new ItemFlowException(ItemFlowException.Reason.NO_PREV_STAGE,
                    "已在头一格（形式核验），再无紧挨的上一格可退");
        }
        int target = current - 1;
        int newRound = before.getCurrentRound() + 1;

        TIchItemFlowRecord rec = new TIchItemFlowRecord();
        rec.setFlowId(flow.getId());
        rec.setStage(target);
        rec.setRoundNo(newRound);
        rec.setAction(ItemFlowChain.ACTION_BACK);
        rec.setNote(note);
        rec.setActionTime(at);
        rec.setDelFlag(0);
        this.ichItemFlowRecordMapper.insert(rec);
        records.add(rec);

        TIchItemFlow patch = new TIchItemFlow();
        patch.setStage(target);
        patch.setRoundNo(newRound);
        patch.setStatus(ItemFlowChain.STATUS_RUNNING);
        patch.setLastAction(ItemFlowChain.ACTION_BACK);
        patch.setUpdateTime(at);

        UpdateWrapper<TIchItemFlow> uw = new UpdateWrapper<>();
        uw.eq("id", flow.getId())
                .eq("stage", current)
                .eq("round_no", before.getCurrentRound())
                .ne("status", ItemFlowChain.STATUS_CLOSED);
        // 退到公示格以前，先前的公示起算压到下面，重走时清空重算
        if (target < 2) {
            uw.set("public_start", null);
        }

        if (this.ichItemFlowMapper.update(patch, uw) == 0) {
            throw new ItemFlowException(ItemFlowException.Reason.CONCURRENT_CHANGED,
                    "申报单刚被别人推动，请按最新格次重新办理");
        }
        mirror(flow, patch);
        if (target < 2) {
            flow.setPublicStart(null);
        }
        return ItemFlowChain.buildView(flow, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemFlowView close(Long id, int closeType, String siteNo, String note, Date now) {
        TIchItemFlow flow = requireFlow(id);
        if (isLocked(flow)) {
            throw new ItemFlowException(ItemFlowException.Reason.LOCKED,
                    "申报单已收口当场锁档，不能再收口");
        }
        if (closeType != ItemFlowChain.CLOSE_CANCEL
                && closeType != ItemFlowChain.CLOSE_TERMINATE) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_CLOSE_TYPE,
                    "收口只认 2注销 3终止 两说");
        }
        List<TIchItemFlowRecord> records = loadRecords(id);
        ItemFlowChain.State before = ItemFlowChain.evaluate(flow, records);
        int current = before.getCurrentStage();
        Date at = now == null ? new Date() : now;

        TIchItemFlow patch = new TIchItemFlow();
        patch.setStatus(ItemFlowChain.STATUS_CLOSED);
        patch.setCloseType(closeType);
        patch.setLastAction(ItemFlowChain.ACTION_CLOSE);
        patch.setUpdateTime(at);

        UpdateWrapper<TIchItemFlow> uw = new UpdateWrapper<>();
        uw.eq("id", flow.getId())
                .eq("stage", current)
                .eq("round_no", before.getCurrentRound())
                .ne("status", ItemFlowChain.STATUS_CLOSED);

        if (closeType == ItemFlowChain.CLOSE_CANCEL) {
            // 注销：所对项目须此刻在册，当场翻成已注销（在册数 −1）
            String no = trim(siteNo);
            if (no.isEmpty()) {
                throw new ItemFlowException(ItemFlowException.Reason.BAD_ARGUMENT,
                        "注销须指明所对的在册项目代号");
            }
            TIchProject project = findActiveProject(no);
            if (project == null) {
                throw new ItemFlowException(ItemFlowException.Reason.BAD_ARGUMENT,
                        "项目 " + no + " 此刻不在册，不能注销");
            }
            project.setStatus(ItemFlowChain.PROJECT_CANCELLED);
            project.setUpdateTime(at);
            this.ichProjectMapper.updateById(project);
            patch.setSiteNo(no);
        }

        TIchItemFlowRecord rec = new TIchItemFlowRecord();
        rec.setFlowId(flow.getId());
        rec.setStage(current);
        rec.setRoundNo(before.getCurrentRound());
        rec.setAction(ItemFlowChain.ACTION_CLOSE);
        rec.setNote(note);
        rec.setActionTime(at);
        rec.setDelFlag(0);
        this.ichItemFlowRecordMapper.insert(rec);
        records.add(rec);

        if (this.ichItemFlowMapper.update(patch, uw) == 0) {
            throw new ItemFlowException(ItemFlowException.Reason.CONCURRENT_CHANGED,
                    "申报单刚被别人推动，请按最新情形重新办理");
        }
        mirror(flow, patch);
        return ItemFlowChain.buildView(flow, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemFlowView revise(Long id, ItemFlowCommand command) {
        TIchItemFlow old = requireFlow(id);
        // 列入之外的变更，只能对着已列入的现行版另起一版
        if (old.getCloseType() == null || old.getCloseType() != ItemFlowChain.CLOSE_LISTED
                || old.getCurrentFlag() == null
                || old.getCurrentFlag() != ItemFlowChain.CURRENT_FLAG_YES) {
            throw new ItemFlowException(ItemFlowException.Reason.NOT_LISTED,
                    "只有已列入的现行版才能另起一版做变更");
        }
        if (command == null) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_ARGUMENT, "变更入参不能为空");
        }
        if (blank(command.getItemName()) || blank(command.getApplyArea())
                || blank(command.getProtectUnit())) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_ARGUMENT,
                    "变更四样仍要齐：项目名称、门类、申报地、保护单位");
        }
        String category = blank(command.getSiteType()) ? old.getSiteType() : trim(command.getSiteType());
        if (!ItemFlowChain.validCategory(category)) {
            throw new ItemFlowException(ItemFlowException.Reason.BAD_CATEGORY,
                    "门类只收民间文学/传统技艺/传统医药/传统音乐");
        }

        // 旧版从现行名录挪开，转到往期那一层给人翻
        TIchItemFlow retire = new TIchItemFlow();
        retire.setCurrentFlag(ItemFlowChain.CURRENT_FLAG_NO);
        retire.setUpdateTime(new Date());
        if (this.ichItemFlowMapper.update(retire,
                new UpdateWrapper<TIchItemFlow>().eq("id", id)
                        .eq("current_flag", ItemFlowChain.CURRENT_FLAG_YES)) == 0) {
            throw new ItemFlowException(ItemFlowException.Reason.CONCURRENT_CHANGED,
                    "现行版刚被别人变更，请刷新后再办");
        }

        // 新版另起：版次加一、从头一格再走，校验码留到重新列入那一刻重算（新旧必不同码）
        TIchItemFlow flow = new TIchItemFlow();
        flow.setBizNo(blank(command.getBizNo()) ? old.getBizNo() : trim(command.getBizNo()));
        flow.setDeclareNo(old.getDeclareNo());
        flow.setVersionNo((old.getVersionNo() == null ? 0 : old.getVersionNo()) + 1);
        flow.setCurrentFlag(ItemFlowChain.CURRENT_FLAG_YES);
        flow.setRoundNo(0);
        flow.setStage(0);
        flow.setStatus(ItemFlowChain.STATUS_IDLE);
        flow.setCloseType(ItemFlowChain.CLOSE_NONE);
        flow.setItemName(trim(command.getItemName()));
        flow.setSiteType(category);
        flow.setApplyArea(trim(command.getApplyArea()));
        flow.setProtectUnit(trim(command.getProtectUnit()));
        flow.setPublicDays(command.getPublicDays() == null ? old.getPublicDays() : command.getPublicDays());
        flow.setSiteNo(old.getSiteNo());
        flow.setPrevVersionId(old.getId());
        flow.setDelFlag(0);
        this.ichItemFlowMapper.insert(flow);

        old.setCurrentFlag(ItemFlowChain.CURRENT_FLAG_NO);
        return ItemFlowChain.buildView(flow, new ArrayList<>());
    }

    @Override
    public ItemFlowView view(Long id) {
        TIchItemFlow flow = requireFlow(id);
        return ItemFlowChain.buildView(flow, loadRecords(id));
    }

    @Override
    public ItemFlowLedger ledger() {
        List<TIchItemFlow> currentFlows = this.ichItemFlowMapper.selectList(
                new QueryWrapper<TIchItemFlow>()
                        .eq("current_flag", ItemFlowChain.CURRENT_FLAG_YES)
                        .eq("del_flag", 0));
        List<TIchProject> projects = this.ichProjectMapper.selectList(
                new QueryWrapper<TIchProject>().eq("del_flag", 0));
        return ItemFlowChain.buildLedger(currentFlows, projects);
    }

    @Override
    public boolean updateContent(Long id, String note) {
        TIchItemFlow flow = requireFlow(id);
        // 收口之后卷面的字改不动
        if (isLocked(flow)) {
            return false;
        }
        TIchItemFlow patch = new TIchItemFlow();
        patch.setContent(note);
        return this.ichItemFlowMapper.update(patch,
                new UpdateWrapper<TIchItemFlow>().eq("id", id)) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TIchItemFlow flow = requireFlow(id);
        // 收口之后整张删不掉；未收口只逻辑删除，留痕不抹
        if (isLocked(flow)) {
            return false;
        }
        TIchItemFlow patch = new TIchItemFlow();
        patch.setDelFlag(1);
        return this.ichItemFlowMapper.update(patch,
                new UpdateWrapper<TIchItemFlow>().eq("id", id)) > 0;
    }

    // ---- 辅助 ----

    private TIchItemFlow requireFlow(Long id) {
        TIchItemFlow flow = this.ichItemFlowMapper.selectById(id);
        if (flow == null || (flow.getDelFlag() != null && flow.getDelFlag() != 0)) {
            throw new ItemFlowException(ItemFlowException.Reason.NOT_FOUND, "查无此申报单: " + id);
        }
        return flow;
    }

    private boolean isLocked(TIchItemFlow flow) {
        return flow.getStatus() != null && flow.getStatus() == ItemFlowChain.STATUS_CLOSED;
    }

    private List<TIchItemFlowRecord> loadRecords(Long id) {
        return this.ichItemFlowRecordMapper.selectList(new QueryWrapper<TIchItemFlowRecord>()
                .eq("flow_id", id)
                .eq("del_flag", 0)
                .orderByAsc("action_time")
                .orderByAsc("id"));
    }

    private boolean hasAdvance(List<TIchItemFlowRecord> records, int stage, int round) {
        for (TIchItemFlowRecord r : records) {
            if (ItemFlowChain.ACTION_ADVANCE.equals(r.getAction())
                    && r.getStage() != null && r.getStage() == stage
                    && r.getRoundNo() != null && r.getRoundNo() == round) {
                return true;
            }
        }
        return false;
    }

    private void assertCodeUnique(String declareNo, String code, Long selfId) {
        List<TIchItemFlow> siblings = this.ichItemFlowMapper.selectList(
                new QueryWrapper<TIchItemFlow>().eq("declare_no", declareNo));
        for (TIchItemFlow f : siblings) {
            if (code.equals(f.getCheckCode()) && !f.getId().equals(selfId)) {
                throw new ItemFlowException(ItemFlowException.Reason.DUPLICATE_CHECK_CODE,
                        "申报 " + declareNo + " 第 " + f.getVersionNo()
                                + " 版已显出同一串校验码，两版同码即错");
            }
        }
    }

    private TIchProject findActiveProject(String siteNo) {
        TIchProject p = this.ichProjectMapper.selectOne(new QueryWrapper<TIchProject>()
                .eq("site_no", siteNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (p == null || p.getStatus() == null
                || p.getStatus() != ItemFlowChain.PROJECT_ACTIVE) {
            return null;
        }
        return p;
    }

    /**
     * 列入即入册：同 siteNo 已在册就沿用（变更再列入不重复造一条），没有则以本版四样造一条在册项目。
     */
    private String ensureProjectActive(TIchItemFlow flow) {
        String siteNo = flow.getSiteNo();
        TIchProject existing = null;
        if (!blank(siteNo)) {
            existing = this.ichProjectMapper.selectOne(new QueryWrapper<TIchProject>()
                    .eq("site_no", siteNo).eq("del_flag", 0).last("limit 1"));
        }
        if (existing == null) {
            TIchProject p = new TIchProject();
            p.setSiteNo(blank(siteNo) ? flow.getDeclareNo() : siteNo);
            p.setSiteName(flow.getItemName());
            p.setSiteType(flow.getSiteType());
            p.setRoadName(flow.getApplyArea());
            p.setStatus(ItemFlowChain.PROJECT_ACTIVE);
            p.setDelFlag(0);
            this.ichProjectMapper.insert(p);
            return p.getSiteNo();
        }
        // 名称正字/换保护单位再列入：底册名字随现行版，保持在册
        existing.setSiteName(flow.getItemName());
        existing.setSiteType(flow.getSiteType());
        existing.setRoadName(flow.getApplyArea());
        existing.setStatus(ItemFlowChain.PROJECT_ACTIVE);
        this.ichProjectMapper.updateById(existing);
        return existing.getSiteNo();
    }

    /** 把落库 patch 反映到内存对象，供同事务内紧接着回算 */
    private void mirror(TIchItemFlow flow, TIchItemFlow patch) {
        if (patch.getStage() != null) {
            flow.setStage(patch.getStage());
        }
        if (patch.getRoundNo() != null) {
            flow.setRoundNo(patch.getRoundNo());
        }
        if (patch.getStatus() != null) {
            flow.setStatus(patch.getStatus());
        }
        if (patch.getCloseType() != null) {
            flow.setCloseType(patch.getCloseType());
        }
        if (patch.getCheckCode() != null) {
            flow.setCheckCode(patch.getCheckCode());
        }
        if (patch.getListedTime() != null) {
            flow.setListedTime(patch.getListedTime());
        }
        if (patch.getSiteNo() != null) {
            flow.setSiteNo(patch.getSiteNo());
        }
        if (patch.getPublicStart() != null) {
            flow.setPublicStart(patch.getPublicStart());
        }
        if (patch.getLastAction() != null) {
            flow.setLastAction(patch.getLastAction());
        }
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
