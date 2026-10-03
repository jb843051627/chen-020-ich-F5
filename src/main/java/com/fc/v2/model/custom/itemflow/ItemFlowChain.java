package com.fc.v2.model.custom.itemflow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.auto.TIchItemFlowRecord;
import com.fc.v2.model.auto.TIchProject;

/**
 * 名录项目申报四格链条的**唯一回算口径**（纯算，不碰库）。
 *
 * <p>四格：0形式核验 → 1专家评议 → 2社会公示 → 3列入名录。规矩全在这里，服务层只负责落库与回算：
 * <ul>
 *   <li>身处第几格靠顺着卷面留痕点已过之格得出；单据格子里的 stage、接口递来的格号都只当意向。</li>
 *   <li>前进只到紧挨的下一格，且当前格门槛得过：核验四样齐、专家意见齐、公示日子走完、名录会议认。</li>
 *   <li>后退只退一格、轮次加一：被退格先前那笔记压到下面，从该格重新攒；更早各格沿用不抹。</li>
 *   <li>收口（列入/注销/终止）即锁档；同格第二遍上报不另起一行，卷面留头一遍那句。</li>
 *   <li>列入一刻算定校验码，改一版重算一码，同申报两版同码即错。</li>
 *   <li>在册项目数：顺着收口单 +1（列入）/−1（注销）点一遍，与底册在册条目同一回算里对得齐。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-03
 */
public final class ItemFlowChain {

    /** 格数：形式核验/专家评议/社会公示/列入名录 */
    public static final int STAGE_COUNT = 4;

    /** 最末一格（列入名录） */
    public static final int LAST_STAGE = STAGE_COUNT - 1;

    /** 申领会落：未起 */
    public static final int STATUS_IDLE = 0;
    /** 申领会落：在办 */
    public static final int STATUS_RUNNING = 1;
    /** 申领会落：已收口（当场锁档） */
    public static final int STATUS_CLOSED = 2;

    /** 收口说法：未收口 */
    public static final int CLOSE_NONE = 0;
    /** 收口说法：列入 */
    public static final int CLOSE_LISTED = 1;
    /** 收口说法：注销（在册项目注销，在册数减一） */
    public static final int CLOSE_CANCEL = 2;
    /** 收口说法：终止（走不到列入中途收口，不进册也不减册） */
    public static final int CLOSE_TERMINATE = 3;

    /** 卷面留痕动作 */
    public static final String ACTION_ADVANCE = "ADVANCE";
    public static final String ACTION_BACK = "BACK";
    public static final String ACTION_CLOSE = "CLOSE";

    /** 底册情形：在册 */
    public static final int PROJECT_ACTIVE = 0;
    /** 底册情形：已注销 */
    public static final int PROJECT_CANCELLED = 1;

    /** 现行名录只露现行版 */
    public static final int CURRENT_FLAG_YES = 1;
    public static final int CURRENT_FLAG_NO = 0;

    private static final String[] STAGE_NAMES = {"形式核验", "专家评议", "社会公示", "列入名录"};

    /** 各门类各按各的排法 */
    private static final List<String> CATEGORIES =
            java.util.Arrays.asList("民间文学", "传统技艺", "传统医药", "传统音乐");

    private ItemFlowChain() {
    }

    public static String stageName(int stage) {
        return stage >= 0 && stage < STAGE_COUNT ? STAGE_NAMES[stage] : "未知格";
    }

    /** 门类是否在四样排法之内 */
    public static boolean validCategory(String category) {
        return category != null && CATEGORIES.contains(category.trim());
    }

    /** 这一格收它要过的门槛名目 */
    public static String gateName(int stage) {
        switch (stage) {
            case 0:
                return "形式四样齐（项目名称、门类、申报地、保护单位）";
            case 1:
                return "专家意见收齐";
            case 2:
                return "公示日子走完";
            case 3:
                return "名录会议认下";
            default:
                throw new IllegalArgumentException("格编号越界: " + stage);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** 一格门槛的回话：过没过，没过卡在哪一句。 */
    public static final class Gate {
        private final boolean passed;
        private final String reason;

        private Gate(boolean passed, String reason) {
            this.passed = passed;
            this.reason = reason;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getReason() {
            return reason;
        }

        static Gate ok() {
            return new Gate(true, "");
        }

        static Gate fail(String reason) {
            return new Gate(false, reason);
        }
    }

    /**
     * 照单据当下所记，判“当前这一格”的门槛过没过（不含已过之格的回算，那由 {@link #evaluate} 管）。
     *
     * @param flow 申报单
     * @param stage 当前格
     * @param now  判门槛那一刻（公示期满按它回看）
     */
    public static Gate gateOf(TIchItemFlow flow, int stage, java.util.Date now) {
        switch (stage) {
            case 0:
                List<String> missing = new ArrayList<>();
                if (!notBlank(flow.getItemName())) {
                    missing.add("项目名称");
                }
                if (!notBlank(flow.getSiteType())) {
                    missing.add("门类");
                } else if (!validCategory(flow.getSiteType())) {
                    return Gate.fail("门类不在民间文学/传统技艺/传统医药/传统音乐四样排法内："
                            + flow.getSiteType());
                }
                if (!notBlank(flow.getApplyArea())) {
                    missing.add("申报地");
                }
                if (!notBlank(flow.getProtectUnit())) {
                    missing.add("保护单位");
                }
                return missing.isEmpty() ? Gate.ok()
                        : Gate.fail("形式核验四样没齐，缺：" + String.join("、", missing));
            case 1:
                return flow.getExpertsReceived() != null && flow.getExpertsReceived() == 1
                        ? Gate.ok() : Gate.fail("专家意见还没收齐");
            case 2:
                if (flow.getPublicStart() == null) {
                    return Gate.fail("尚未进社会公示格，没起算公示日子");
                }
                Integer days = flow.getPublicDays();
                if (days == null || days.intValue() <= 0) {
                    return Gate.fail("当地定的公示几日没填，日子没法走完");
                }
                long deadline = flow.getPublicStart().getTime() + days.longValue() * 86_400_000L;
                if (now == null || now.getTime() < deadline) {
                    return Gate.fail("公示日子没走完，材料再齐后一格开着也不能进");
                }
                return Gate.ok();
            case 3:
                return flow.getMeetingRecognized() != null && flow.getMeetingRecognized() == 1
                        ? Gate.ok() : Gate.fail("名录会议还没认");
            default:
                throw new IllegalArgumentException("格编号越界: " + stage);
        }
    }

    /** 一张单顺着留痕回算出的链条状态（纯算）。 */
    public static final class State {
        private final int currentRound;
        private final int entryStage;
        private final boolean[] passed;
        private final int currentStage;
        private final boolean closed;
        private final List<TIchItemFlowRecord> ordered;

        State(int currentRound, int entryStage, boolean[] passed, int currentStage,
              boolean closed, List<TIchItemFlowRecord> ordered) {
            this.currentRound = currentRound;
            this.entryStage = entryStage;
            this.passed = passed;
            this.currentStage = currentStage;
            this.closed = closed;
            this.ordered = ordered;
        }

        public int getCurrentRound() {
            return currentRound;
        }

        /** 本轮重走从哪一格重新攒（本轮最早触到的格；头一回为 0） */
        public int getEntryStage() {
            return entryStage;
        }

        public boolean isPassed(int stage) {
            return stage >= 0 && stage < STAGE_COUNT && passed[stage];
        }

        /** 当前停在第几格（顺已过之格点出） */
        public int getCurrentStage() {
            return currentStage;
        }

        /** 四格门槛是否全过（推进到列入） */
        public boolean isAllPassed() {
            for (int s = 0; s < STAGE_COUNT; s++) {
                if (!passed[s]) {
                    return false;
                }
            }
            return true;
        }

        public boolean isClosed() {
            return closed;
        }

        public List<TIchItemFlowRecord> getOrdered() {
            return ordered;
        }
    }

    /**
     * 顺着单据与全部卷面留痕回算当前停在第几格。
     *
     * <p>入口格以前各格沿用早先轮次已过的结果；入口格及以后只认本轮的入格行。
     * 后退只把被退格那一笔压到下面（轮次加一），更早各格一笔不抹。
     */
    public static State evaluate(TIchItemFlow flow, List<TIchItemFlowRecord> records) {
        int currentRound = flow.getRoundNo() == null ? 0 : flow.getRoundNo();
        boolean closed = flow.getStatus() != null && flow.getStatus() == STATUS_CLOSED;

        List<TIchItemFlowRecord> ordered = new ArrayList<>(
                records == null ? Collections.emptyList() : records);
        ordered.sort(Comparator
                .comparing(TIchItemFlowRecord::getActionTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(r -> r.getId() == null ? 0L : r.getId()));

        // 本轮重走入口格：本轮最早触到的格（后退目标或入格行）；一笔没有则从头一格起
        int entryStage = STAGE_COUNT;
        for (TIchItemFlowRecord r : ordered) {
            if (r.getRoundNo() != null && r.getRoundNo() == currentRound
                    && r.getStage() != null && r.getStage() < entryStage) {
                entryStage = r.getStage();
            }
        }
        if (entryStage >= STAGE_COUNT) {
            entryStage = 0;
        }

        boolean[] passed = new boolean[STAGE_COUNT];
        for (int s = 0; s < STAGE_COUNT; s++) {
            for (TIchItemFlowRecord r : ordered) {
                if (r.getStage() == null || r.getStage() != s
                        || !ACTION_ADVANCE.equals(r.getAction())) {
                    continue;
                }
                int rr = r.getRoundNo() == null ? 0 : r.getRoundNo();
                boolean effective = s < entryStage || rr == currentRound;
                if (effective) {
                    passed[s] = true;
                    break;
                }
            }
        }

        // 只能一格接一格：第一个没过的格即当前格；四格都过停在列入格
        int currentStage = LAST_STAGE;
        for (int s = 0; s < STAGE_COUNT; s++) {
            if (!passed[s]) {
                currentStage = s;
                break;
            }
        }
        return new State(currentRound, entryStage, passed, currentStage, closed,
                Collections.unmodifiableList(ordered));
    }

    /** 这一格头一遍上报留的那句（同格第二遍不另起一行，仍取头一遍）。 */
    public static String firstNote(State st, int stage) {
        for (TIchItemFlowRecord r : st.getOrdered()) {
            if (r.getStage() != null && r.getStage() == stage
                    && ACTION_ADVANCE.equals(r.getAction())) {
                int rr = r.getRoundNo() == null ? 0 : r.getRoundNo();
                if (stage < st.getEntryStage() || rr == st.getCurrentRound()) {
                    return r.getNote();
                }
            }
        }
        return null;
    }

    /**
     * 一张单此刻全貌：当前格顺留痕回算，四格门槛逐格摆，页面格号只作参考、相左听回算。
     */
    public static ItemFlowView buildView(TIchItemFlow flow, List<TIchItemFlowRecord> records) {
        State st = evaluate(flow, records);
        ItemFlowView v = new ItemFlowView();
        v.setFlowId(String.valueOf(flow.getId()));
        v.setBizNo(flow.getBizNo());
        v.setDeclareNo(flow.getDeclareNo());
        v.setVersionNo(flow.getVersionNo() == null ? 0 : flow.getVersionNo());
        v.setCurrentFlag(flow.getCurrentFlag() == null ? CURRENT_FLAG_YES : flow.getCurrentFlag());
        v.setRoundNo(st.getCurrentRound());
        v.setStatus(flow.getStatus() == null ? STATUS_IDLE : flow.getStatus());
        v.setCloseType(flow.getCloseType() == null ? CLOSE_NONE : flow.getCloseType());
        v.setLocked(st.isClosed());
        v.setCheckCode(flow.getCheckCode());
        v.setSiteNo(flow.getSiteNo());
        v.setDisplayedStage(flow.getStage());

        List<ItemFlowView.StageView> stageViews = new ArrayList<>();
        for (int s = 0; s < STAGE_COUNT; s++) {
            ItemFlowView.StageView sv = new ItemFlowView.StageView();
            sv.setStage(s);
            sv.setStageName(stageName(s));
            sv.setPassed(st.isPassed(s));
            sv.setNote(firstNote(st, s));
            stageViews.add(sv);
        }
        v.setStages(stageViews);

        int current = st.getCurrentStage();
        v.setCurrentStage(current);
        v.setCurrentStageName(stageName(current));
        v.setStageMatched(flow.getStage() != null && flow.getStage() == current);
        return v;
    }

    /**
     * 列入一刻算定的校验码：同一份申报的身份要件 + 版次入算，SHA-256 取十六进制。
     * 名称正字、换保护单位、版次加一，任一处不同码便不同；两版显出同一串码即错。
     */
    public static String checkCode(TIchItemFlow flow) {
        String canonical = joinFields(
                nz(flow.getDeclareNo()),
                String.valueOf(flow.getVersionNo() == null ? 0 : flow.getVersionNo()),
                nz(flow.getItemName()),
                nz(flow.getSiteType()),
                nz(flow.getApplyArea()),
                nz(flow.getProtectUnit()),
                nz(flow.getBizNo()));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("算不出申报单校验码", e);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String joinFields(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(p.replace("\\", "\\\\").replace("|", "\\|")).append('|');
        }
        return sb.toString();
    }

    /**
     * 册子一本账：在册项目数两处在同一回算里点一遍，对得齐才算。
     *
     * @param currentFlows 现行版（current_flag=1）全部申报单（往期版不重复计数）
     * @param projects     底册未删条目
     */
    public static ItemFlowLedger buildLedger(List<TIchItemFlow> currentFlows,
                                             List<TIchProject> projects) {
        // 头一处：顺着收口单逐格点——列入 +1、注销 −1；终止未入册不减
        int flowIn = 0;
        int flowOut = 0;
        Map<String, String> codeByDeclare = new LinkedHashMap<>();
        for (TIchItemFlow f : currentFlows == null ? Collections.<TIchItemFlow>emptyList() : currentFlows) {
            int ct = f.getCloseType() == null ? CLOSE_NONE : f.getCloseType();
            if (ct == CLOSE_LISTED) {
                flowIn++;
            } else if (ct == CLOSE_CANCEL) {
                flowOut++;
            }
            if (f.getDeclareNo() != null && f.getCheckCode() != null) {
                codeByDeclare.put(f.getDeclareNo(), f.getCheckCode());
            }
        }
        int flowRegistered = flowIn - flowOut;

        // 另一处：当场逐格点底册在册条目，与上一处须是同一回算
        int registryRegistered = 0;
        for (TIchProject p : projects == null ? Collections.<TIchProject>emptyList() : projects) {
            if ((p.getDelFlag() == null || p.getDelFlag() == 0)
                    && p.getStatus() != null && p.getStatus() == PROJECT_ACTIVE) {
                registryRegistered++;
            }
        }

        ItemFlowLedger l = new ItemFlowLedger();
        l.setListedCount(flowIn);
        l.setCancelledCount(flowOut);
        l.setFlowRegisteredCount(flowRegistered);
        l.setRegistryRegisteredCount(registryRegistered);
        l.setCountMatched(flowRegistered == registryRegistered);
        return l;
    }
}
