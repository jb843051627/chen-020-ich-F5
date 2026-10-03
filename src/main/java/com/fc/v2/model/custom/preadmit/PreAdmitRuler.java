package com.fc.v2.model.custom.preadmit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TIchPreLine;

/**
 * 申报准入的**唯一一把尺**（纯算，不碰库）。
 *
 * <p>在场名单、单头判定、两头合判、立线校验全从这里过，服务层只负责装载与落库：
 * <ul>
 *   <li><b>在场</b>：未删、情形为在场（空值按在场）、已到启用之日（含）、未到交棒之日
 *       （不含）。交棒那一栏空着按一直在场认，不许因缺这一栏把整条当没有。
 *       前后两条交替那一日，旧的当日出去、新的当日进来，同一刻两处问，答案同一句。</li>
 *   <li><b>选线</b>：让位顺位高的说话；顺位相同，代号字数靠后的赢；再相同按代号串、
 *       主键定死，回两次必须是同一个答案。</li>
 *   <li><b>分层</b>：从低到高起分、过线、优线三条分界数，比到哪一段就往哪一层去；
 *       过线层与优线层才算过，起分层是没够着过线，越层是冲过优线。</li>
 *   <li><b>两头看</b>：本人从艺年数走一条线、带徒人数另有一条线管着，两头各自都过
 *       才算过；哪一头没过就亮哪一头的分界数与实际数。</li>
 *   <li><b>无结论</b>：当刻一条线也够不着，线代号与启用之日两栏空着，
 *       不许抓邻近那条的数凑答复。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-03
 */
public final class PreAdmitRuler {

    /** 头：本人从艺年数 */
    public static final int HEAD_YEARS = 0;

    /** 头：带徒人数 */
    public static final int HEAD_APPRENTICES = 1;

    /** 头编号即下标 */
    private static final String[] HEAD_NAMES = {"从艺年数", "带徒人数"};

    /** 起分层（没够着过线） */
    public static final int TIER_START = 1;

    /** 过线层 */
    public static final int TIER_PASS = 2;

    /** 优线层 */
    public static final int TIER_EXCELLENT = 3;

    /** 越层（冲过优线） */
    public static final int TIER_OVER = 4;

    private PreAdmitRuler() {
    }

    public static String headName(int headNo) {
        return headNo >= 0 && headNo < HEAD_NAMES.length ? HEAD_NAMES[headNo] : "未知头";
    }

    public static String tierName(int tier) {
        if (tier == TIER_START) {
            return "起分层";
        }
        if (tier == TIER_PASS) {
            return "过线层";
        }
        if (tier == TIER_EXCELLENT) {
            return "优线层";
        }
        if (tier == TIER_OVER) {
            return "越层";
        }
        return "未知层";
    }

    /**
     * 一条线在 at 时刻是否在场——唯一覆盖谓词，半开区间 [启用之日, 交棒之日)。
     * 交棒日空着按一直在场；情形空值按在场。
     */
    public static boolean activeAt(TIchPreLine r, Date at) {
        if (r == null || at == null) {
            return false;
        }
        if (r.getDelFlag() != null && r.getDelFlag() != 0) {
            return false;
        }
        if (r.getStatus() != null && r.getStatus() != 0) {
            return false;
        }
        if (r.getEffStart() != null && at.before(r.getEffStart())) {
            return false;
        }
        if (r.getEffEnd() != null && !at.before(r.getEffEnd())) {
            return false;
        }
        return true;
    }

    /**
     * 一条线立不立得起来（复算口径，与旧线册服务兼容）：代号、线名与三条分界数
     * 缺不得、分界数要从低到高。启用之日、让位顺位在旧卷里可能空着——
     * 空启用日按自始在场、空顺位按末位，不许因这两栏把整条当没有；
     * 交棒日空着按一直在场。新立线时 {@link #validateLine} 仍逐栏必填。
     */
    public static boolean isStanding(TIchPreLine r) {
        return r != null
                && notBlank(r.getRuleCode())
                && notBlank(r.getRuleName())
                && r.getTh1Max() != null
                && r.getTh2Max() != null
                && r.getTh3Max() != null
                && thresholdsOrdered(r) == null;
    }

    /** 三条分界数从低到高且不为负；不像话时回一句说法，像样时回 null */
    public static String thresholdsOrdered(TIchPreLine r) {
        if (r.getTh1Max() == null || r.getTh2Max() == null || r.getTh3Max() == null) {
            return "三条分界数一条都缺不得";
        }
        if (r.getTh1Max().signum() < 0 || r.getTh2Max().signum() < 0 || r.getTh3Max().signum() < 0) {
            return "分界数不像话：不许为负数";
        }
        if (!(r.getTh1Max().compareTo(r.getTh2Max()) < 0
                && r.getTh2Max().compareTo(r.getTh3Max()) < 0)) {
            return "三条分界数须从低到高：起分 < 过线 < 优线";
        }
        return null;
    }

    /** 让位顺位降序；并列时代号字数靠后的赢；再并列按代号串、主键定死 */
    private static final Comparator<TIchPreLine> LINE_ORDER = new Comparator<TIchPreLine>() {
        @Override
        public int compare(TIchPreLine a, TIchPreLine b) {
            int pa = a.getPriority() == null ? Integer.MIN_VALUE : a.getPriority();
            int pb = b.getPriority() == null ? Integer.MIN_VALUE : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            if (ca.length() != cb.length()) {
                return cb.length() - ca.length();
            }
            int byCode = cb.compareTo(ca);
            if (byCode != 0) {
                return byCode;
            }
            long ia = a.getId() == null ? Long.MIN_VALUE : a.getId();
            long ib = b.getId() == null ? Long.MIN_VALUE : b.getId();
            return Long.compare(ib, ia);
        }
    };

    /**
     * at 时刻在场且立得起来的线，按唯一选线次序排好。
     * 已停用的、过了交棒日的、没到启用日的、缺栏立不起来的都不进场。
     */
    public static List<TIchPreLine> presentAt(List<TIchPreLine> all, Date at) {
        List<TIchPreLine> present = new ArrayList<>();
        if (all == null || at == null) {
            return present;
        }
        for (TIchPreLine r : all) {
            if (activeAt(r, at) && isStanding(r)) {
                present.add(r);
            }
        }
        present.sort(LINE_ORDER);
        return present;
    }

    /** 在场名单里说话的那一条（次序最前者）；一条也够不着回 null */
    public static TIchPreLine topOf(List<TIchPreLine> presentSorted) {
        return presentSorted == null || presentSorted.isEmpty() ? null : presentSorted.get(0);
    }

    /**
     * 来历核对：在场名单里有没有代号相同、启用之日也同为钉下那一日的那条。
     * 代号在册中查无、或该日不在场（已停用/已交棒/启用日对不上）都算来历不对。
     */
    public static TIchPreLine findPresent(List<TIchPreLine> presentSorted, String ruleCode, Date effStart) {
        if (presentSorted == null || ruleCode == null) {
            return null;
        }
        for (TIchPreLine r : presentSorted) {
            if (ruleCode.equals(r.getRuleCode()) && sameInstant(r.getEffStart(), effStart)) {
                return r;
            }
        }
        return null;
    }

    private static boolean sameInstant(Date a, Date b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    /**
     * 判一头。在场一条也够不着：无结论，线代号与启用之日空着，
     * 不许抓邻近那条的数凑答复。
     */
    public static HeadJudge judgeHead(int headNo, BigDecimal metric, List<TIchPreLine> presentSorted) {
        HeadJudge h = new HeadJudge();
        h.setHeadNo(headNo);
        h.setHeadName(headName(headNo));
        h.setMetric(metric);
        TIchPreLine line = topOf(presentSorted);
        if (line == null) {
            h.setAvailable(false);
            return h;
        }
        h.setAvailable(true);
        h.setRuleId(line.getId());
        h.setRuleCode(line.getRuleCode());
        h.setRuleName(line.getRuleName());
        h.setEffStart(line.getEffStart());
        h.setTh1Max(line.getTh1Max());
        h.setTh2Max(line.getTh2Max());
        h.setTh3Max(line.getTh3Max());
        if (metric == null) {
            // 申报这一头没填数：判不了层，按无结论回话，不替申报人凑数
            h.setAvailable(false);
            h.setRuleId(null);
            h.setRuleCode(null);
            h.setRuleName(null);
            h.setEffStart(null);
            h.setTh1Max(null);
            h.setTh2Max(null);
            h.setTh3Max(null);
            return h;
        }
        int tier = tierOf(line, metric);
        h.setTier(tier);
        boolean passed = tier == TIER_PASS || tier == TIER_EXCELLENT;
        h.setPassed(passed);
        if (!passed) {
            h.setPrompt(promptOf(headNo, metric, line, tier));
        }
        return h;
    }

    /** 比到哪一段就往哪一层去：等于分界取这一层（≤起分=起分层，其上类推，冲过优线=越层） */
    public static int tierOf(TIchPreLine line, BigDecimal metric) {
        if (metric.compareTo(line.getTh1Max()) <= 0) {
            return TIER_START;
        }
        if (metric.compareTo(line.getTh2Max()) <= 0) {
            return TIER_PASS;
        }
        if (metric.compareTo(line.getTh3Max()) <= 0) {
            return TIER_EXCELLENT;
        }
        return TIER_OVER;
    }

    /** 没过的提示：哪一头越了就把那一头的分界数与实际数亮出来 */
    private static String promptOf(int headNo, BigDecimal metric, TIchPreLine line, int tier) {
        String head = headName(headNo);
        if (tier == TIER_START) {
            return head + "报 " + metric.stripTrailingZeros().toPlainString()
                    + "，没够着过线 " + line.getTh2Max().stripTrailingZeros().toPlainString()
                    + "（起分 " + line.getTh1Max().stripTrailingZeros().toPlainString() + "，落到起分层）";
        }
        // TIER_OVER
        return head + "报 " + metric.stripTrailingZeros().toPlainString()
                + "，冲过优线 " + line.getTh3Max().stripTrailingZeros().toPlainString()
                + "（落到越层）";
    }

    /**
     * 一份申报两头合判：两头共用同一份在场名单（同一回算），各走各的数。
     * 任一头无结论即整份无结论；两头都在线段内才准入；否则不准入，
     * 没过的那一头提示已落在明细里。
     */
    public static DeclareVerdict declare(PreDeclareInput input, List<TIchPreLine> presentSorted) {
        DeclareVerdict v = new DeclareVerdict();
        v.setApplicantCode(input == null ? null : input.getApplicantCode());
        v.setApplyAt(input == null ? null : input.getApplyAt());
        if (input == null || input.getApplyAt() == null) {
            return v;
        }
        HeadJudge years = judgeHead(HEAD_YEARS, input.getYears(), presentSorted);
        HeadJudge apprentices = judgeHead(HEAD_APPRENTICES, input.getApprentices(), presentSorted);
        List<HeadJudge> heads = new ArrayList<>();
        heads.add(years);
        heads.add(apprentices);
        v.setHeads(heads);
        if (!years.isAvailable() || !apprentices.isAvailable()) {
            v.setVerdict(DeclareVerdict.VERDICT_NONE);
        } else if (Boolean.TRUE.equals(years.getPassed()) && Boolean.TRUE.equals(apprentices.getPassed())) {
            v.setVerdict(DeclareVerdict.VERDICT_PASS);
        } else {
            v.setVerdict(DeclareVerdict.VERDICT_REJECT);
        }
        return v;
    }

    /**
     * 立线校验：逐栏点名空着的与数字不像话的，按册子栏目顺序回执。
     * 线名、代号与三条分界数缺不得；启用之日、交棒之日、让位顺位空着不记一笔——
     * 两栏日子空着按一直在场认，顺位空着按末位，不许因缺这一栏把整条当没有。
     */
    public static List<PreLineSaveReceipt.FieldError> validateLine(TIchPreLine form) {
        List<PreLineSaveReceipt.FieldError> errors = new ArrayList<>();
        if (form == null) {
            errors.add(new PreLineSaveReceipt.FieldError("line", "线册一行不能整行空着"));
            return errors;
        }
        if (!notBlank(form.getRuleName())) {
            errors.add(new PreLineSaveReceipt.FieldError("ruleName", "线名这一栏空着"));
        }
        if (!notBlank(form.getRuleCode())) {
            errors.add(new PreLineSaveReceipt.FieldError("ruleCode", "代号这一栏空着"));
        }
        if (form.getTh1Max() == null) {
            errors.add(new PreLineSaveReceipt.FieldError("th1Max", "起分这一栏空着"));
        }
        if (form.getTh2Max() == null) {
            errors.add(new PreLineSaveReceipt.FieldError("th2Max", "过线这一栏空着"));
        }
        if (form.getTh3Max() == null) {
            errors.add(new PreLineSaveReceipt.FieldError("th3Max", "优线这一栏空着"));
        }
        String orderMsg = thresholdsOrdered(form);
        if (orderMsg != null && form.getTh1Max() != null
                && form.getTh2Max() != null && form.getTh3Max() != null) {
            errors.add(new PreLineSaveReceipt.FieldError("thresholds", orderMsg));
        }
        // 两栏日子空着按一直在场认；两栏都填了，交棒日须晚于启用日
        if (form.getEffEnd() != null && form.getEffStart() != null
                && !form.getEffEnd().after(form.getEffStart())) {
            errors.add(new PreLineSaveReceipt.FieldError("effEnd",
                    "交棒之日不像话：须晚于启用之日（空着按一直在场认）"));
        }
        return errors;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** 只读空单，便于调用方少 new 一个集合 */
    public static List<TIchPreLine> emptyPresent() {
        return Collections.emptyList();
    }
}
