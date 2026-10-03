package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIchPreDeclareHeadMapper;
import com.fc.v2.mapper.auto.TIchPreDeclareMapper;
import com.fc.v2.mapper.auto.TIchPreLineMapper;
import com.fc.v2.model.auto.TIchPreDeclare;
import com.fc.v2.model.auto.TIchPreDeclareHead;
import com.fc.v2.model.auto.TIchPreLine;
import com.fc.v2.model.custom.preadmit.DeclareVerdict;
import com.fc.v2.model.custom.preadmit.HeadJudge;
import com.fc.v2.model.custom.preadmit.PreAdmitReport;
import com.fc.v2.model.custom.preadmit.PreAdmitRuler;
import com.fc.v2.model.custom.preadmit.PreDeclareInput;
import com.fc.v2.model.custom.preadmit.PreLineSaveReceipt;
import com.fc.v2.model.custom.preadmit.StaleHeadRef;
import com.fc.v2.service.ITIchPreAdmitService;

/**
 * 传承人申报准入 Service业务层处理（dual-threshold 形状：从艺年数/带徒人数两头看）。
 *
 * <p>判定只有这一条路：装载线册 → {@link PreAdmitRuler} 回算 → 回话原样落库。
 * 落库的线代号与启用之日以尺的回话为准，页面不许先摆答案再回头补依据。
 * 按线代号取值的老路（{@link TIchPreLineServiceImpl}）一字不动。
 *
 * @author fuce
 * @date 2026-10-03
 */
@Service
public class TIchPreAdmitServiceImpl implements ITIchPreAdmitService {

    /** 来历不对：代号在整本册子里查无 */
    public static final String STALE_CODE_MISSING = "线代号在册中查无";

    /** 来历不对：代号在册，但申报那一天它不在场（已停用或已交棒） */
    public static final String STALE_NOT_PRESENT = "该日此线不在场（已停用或已交棒）";

    @javax.annotation.Resource
    private TIchPreLineMapper ichPreLineMapper;

    @javax.annotation.Resource
    private TIchPreDeclareMapper ichPreDeclareMapper;

    @javax.annotation.Resource
    private TIchPreDeclareHeadMapper ichPreDeclareHeadMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeclareVerdict judgeOne(PreDeclareInput input) {
        if (input == null || input.getApplyAt() == null) {
            // 没注明回看那一刻，不判也不落——不许拿今天的线顶上去
            return new DeclareVerdict();
        }
        List<TIchPreLine> lines = loadLines();
        DeclareVerdict verdict = PreAdmitRuler.declare(input, PreAdmitRuler.presentAt(lines, input.getApplyAt()));
        persist(verdict);
        return verdict;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreAdmitReport judgeBatch(List<PreDeclareInput> inputs, Date at) {
        // 线册只装载这一回：在场名单与逐条判定同源于这一份快照，两回的数不一样即错
        List<TIchPreLine> lines = loadLines();

        PreAdmitReport report = new PreAdmitReport();
        report.setAt(at);
        if (at != null) {
            report.setPresentLines(PreAdmitRuler.presentAt(lines, at));
        }

        List<DeclareVerdict> verdicts = new ArrayList<>();
        int admitted = 0;
        int rejected = 0;
        int noConclusion = 0;
        if (inputs != null) {
            for (PreDeclareInput input : inputs) {
                // 每份申报按它自己注明的那一刻回看同一份快照：前年的复算拿前年的日子，
                // 手里最新那条不许顶上去
                DeclareVerdict verdict;
                if (input == null || input.getApplyAt() == null) {
                    verdict = new DeclareVerdict();
                    if (input != null) {
                        verdict.setApplicantCode(input.getApplicantCode());
                    }
                } else {
                    verdict = PreAdmitRuler.declare(input,
                            PreAdmitRuler.presentAt(lines, input.getApplyAt()));
                    persist(verdict);
                }
                verdicts.add(verdict);
                if (verdict.getVerdict() == DeclareVerdict.VERDICT_PASS) {
                    admitted++;
                } else if (verdict.getVerdict() == DeclareVerdict.VERDICT_REJECT) {
                    rejected++;
                } else {
                    noConclusion++;
                }
            }
        }
        report.setVerdicts(verdicts);
        report.setJudged(verdicts.size());
        report.setAdmitted(admitted);
        report.setRejected(rejected);
        report.setNoConclusion(noConclusion);
        return report;
    }

    @Override
    public List<TIchPreLine> presentLines(Date at) {
        if (at == null) {
            return new ArrayList<>();
        }
        return PreAdmitRuler.presentAt(loadLines(), at);
    }

    @Override
    public TIchPreLine locateAt(Date at) {
        if (at == null) {
            return null;
        }
        return PreAdmitRuler.topOf(PreAdmitRuler.presentAt(loadLines(), at));
    }

    @Override
    public List<StaleHeadRef> listStaleRefs() {
        List<TIchPreLine> lines = loadLines();
        List<TIchPreDeclare> declares = ichPreDeclareMapper.selectList(
                new QueryWrapper<TIchPreDeclare>().eq("del_flag", 0));
        List<TIchPreDeclareHead> heads = ichPreDeclareHeadMapper.selectList(
                new QueryWrapper<TIchPreDeclareHead>().eq("del_flag", 0));
        Map<Long, TIchPreDeclare> declareById = new HashMap<>();
        for (TIchPreDeclare d : declares) {
            declareById.put(d.getId(), d);
        }

        List<StaleHeadRef> stale = new ArrayList<>();
        // 同一申报日的在场名单只回看一回
        Map<Date, List<TIchPreLine>> presentByDay = new HashMap<>();
        for (TIchPreDeclareHead h : heads) {
            if (isBlank(h.getRuleCode())) {
                // 无结论那两栏本就空着，不算来历不对
                continue;
            }
            TIchPreDeclare d = declareById.get(h.getDeclareId());
            if (d == null || d.getApplyAt() == null) {
                continue;
            }
            List<TIchPreLine> present = presentByDay.get(d.getApplyAt());
            if (present == null) {
                present = PreAdmitRuler.presentAt(lines, d.getApplyAt());
                presentByDay.put(d.getApplyAt(), present);
            }
            TIchPreLine hit = PreAdmitRuler.findPresent(present, h.getRuleCode(), h.getEffStart());
            if (hit != null) {
                continue;
            }
            stale.add(toStaleRef(d, h, codeExists(lines, h.getRuleCode())
                    ? STALE_NOT_PRESENT : STALE_CODE_MISSING));
        }
        return stale;
    }

    @Override
    public PreLineSaveReceipt appendLine(TIchPreLine form) {
        PreLineSaveReceipt receipt = new PreLineSaveReceipt();
        List<PreLineSaveReceipt.FieldError> errors = PreAdmitRuler.validateLine(form);
        if (!errors.isEmpty()) {
            // 三条分界数缺一条都立不起来；回执逐栏点名，光回一句"数不对"不算交代
            receipt.setSaved(false);
            receipt.setErrors(errors);
            return receipt;
        }
        form.setStatus(0);
        form.setDelFlag(0);
        if (form.getCreateTime() == null) {
            form.setCreateTime(new Date());
        }
        this.ichPreLineMapper.insert(form);
        receipt.setSaved(true);
        receipt.setLine(form);
        return receipt;
    }

    @Override
    public int retireLine(Long id) {
        if (id == null) {
            return 0;
        }
        // 按下不等于撕掉：在册里仍翻得到，只是不进现行判定
        TIchPreLine patch = new TIchPreLine();
        patch.setId(id);
        patch.setStatus(1);
        patch.setUpdateTime(new Date());
        return this.ichPreLineMapper.updateById(patch);
    }

    @Override
    public List<TIchPreDeclare> listDeclares(String applicantCode) {
        QueryWrapper<TIchPreDeclare> qw = new QueryWrapper<TIchPreDeclare>()
                .eq("del_flag", 0)
                .orderByDesc("apply_at")
                .orderByDesc("id");
        if (!isBlank(applicantCode)) {
            qw.eq("applicant_code", applicantCode.trim());
        }
        return this.ichPreDeclareMapper.selectList(qw);
    }

    @Override
    public DeclareVerdict loadVerdict(Long declareId) {
        DeclareVerdict v = new DeclareVerdict();
        if (declareId == null) {
            return v;
        }
        TIchPreDeclare main = ichPreDeclareMapper.selectById(declareId);
        if (main == null || (main.getDelFlag() != null && main.getDelFlag() != 0)) {
            return v;
        }
        v.setApplicantCode(main.getApplicantCode());
        v.setApplyAt(main.getApplyAt());
        v.setVerdict(main.getVerdict() == null ? DeclareVerdict.VERDICT_NONE : main.getVerdict());
        List<TIchPreDeclareHead> rows = ichPreDeclareHeadMapper.selectList(
                new QueryWrapper<TIchPreDeclareHead>()
                        .eq("declare_id", declareId)
                        .eq("del_flag", 0)
                        .orderByAsc("head_no"));
        List<HeadJudge> heads = new ArrayList<>();
        for (TIchPreDeclareHead r : rows) {
            HeadJudge h = new HeadJudge();
            h.setHeadNo(r.getHeadNo() == null ? -1 : r.getHeadNo());
            h.setHeadName(r.getHeadName());
            h.setMetric(r.getMetric());
            if (!isBlank(r.getRuleCode())) {
                // 旧档里钉死的字原样取回：换了线也不重判，先前那头的提示不许在页面上不走
                h.setAvailable(true);
                h.setRuleCode(r.getRuleCode());
                h.setEffStart(r.getEffStart());
                h.setTier(r.getTier());
                h.setPassed(r.getPassed() == null ? null : r.getPassed() == 1);
                h.setPrompt(r.getPrompt());
            }
            heads.add(h);
        }
        v.setHeads(heads);
        return v;
    }

    /** 结论落库：主表一笔、两头各一笔；线代号与启用之日以尺的回话原样钉死 */
    private void persist(DeclareVerdict verdict) {
        TIchPreDeclare main = new TIchPreDeclare();
        main.setApplicantCode(verdict.getApplicantCode());
        main.setApplyAt(verdict.getApplyAt());
        main.setVerdict(verdict.getVerdict());
        main.setHeadTotal(verdict.getHeads().size());
        main.setDelFlag(0);
        this.ichPreDeclareMapper.insert(main);

        main.setDeclareNo("SB" + main.getId());
        this.ichPreDeclareMapper.updateById(main);

        for (HeadJudge h : verdict.getHeads()) {
            TIchPreDeclareHead row = new TIchPreDeclareHead();
            row.setDeclareId(main.getId());
            row.setHeadNo(h.getHeadNo());
            row.setHeadName(h.getHeadName());
            row.setMetric(h.getMetric());
            if (h.isAvailable()) {
                // 钉死的字段以出口回话为准；后来换了线也不回头改这一段
                row.setRuleCode(h.getRuleCode());
                row.setEffStart(h.getEffStart());
                row.setTier(h.getTier());
                row.setPassed(Boolean.TRUE.equals(h.getPassed()) ? 1 : 0);
                row.setPrompt(h.getPrompt());
            }
            // 无结论：代号与启用之日两栏空着，tier/passed/prompt 亦空，不抓邻近线凑数
            row.setDelFlag(0);
            this.ichPreDeclareHeadMapper.insert(row);
        }
    }

    private StaleHeadRef toStaleRef(TIchPreDeclare d, TIchPreDeclareHead h, String reason) {
        StaleHeadRef s = new StaleHeadRef();
        s.setDeclareId(String.valueOf(h.getDeclareId()));
        s.setDeclareNo(d.getDeclareNo());
        s.setApplicantCode(d.getApplicantCode());
        s.setApplyAt(d.getApplyAt());
        s.setHeadNo(h.getHeadNo() == null ? -1 : h.getHeadNo());
        s.setHeadName(h.getHeadName());
        s.setMetric(h.getMetric());
        s.setPinnedRuleCode(h.getRuleCode());
        s.setPinnedEffStart(h.getEffStart());
        s.setReason(reason);
        return s;
    }

    private List<TIchPreLine> loadLines() {
        return this.ichPreLineMapper.selectList(new QueryWrapper<TIchPreLine>().eq("del_flag", 0));
    }

    private boolean codeExists(List<TIchPreLine> lines, String code) {
        for (TIchPreLine r : lines) {
            if (code.equals(r.getRuleCode())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
