package com.fc.v2.model.custom.itemflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.auto.TIchItemFlowRecord;
import com.fc.v2.util.MD5Util;

/**
 * 名录项目申报四格流转的**唯一回算口径**（纯算，不碰库）。
 *
 * <p>规矩全在这里，服务层只负责装载与落库：
 * <ul>
 *   <li>四格：0形式核验、1专家评议、2社会公示、3列入名录；前进只到挨着的那一格。</li>
 *   <li>各格门槛：核验要四样齐（项目名称、门类、申报地、保护单位，门类各按各的排法）；
 *       评议看专家意见收没收齐；公示守着当地定的公示几日，日子没走完材料再齐也不算，
 *       后一格开着也不能进；列入看名录会议认不认。</li>
 *   <li>身处第几格不靠纸上写的格号，靠逐格点已过之格得出：顺着留痕表一笔笔点，
 *       自核验起第一个没有现行过口留痕的格即当前格；被后退压到下面的旧记不算。</li>
 *   <li>收口三说（列入/注销/终止）一经落定即锁档；列入校验码那一刻钉死，改版重算，
 *       新旧两版绝不同码。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-03
 */
public final class ItemFlowRuler {

    /** 形式核验 */
    public static final int STAGE_VERIFY = 0;

    /** 专家评议 */
    public static final int STAGE_REVIEW = 1;

    /** 社会公示 */
    public static final int STAGE_PUBLIC = 2;

    /** 列入名录 */
    public static final int STAGE_LIST = 3;

    /** 格数 */
    public static final int STAGE_COUNT = 4;

    /** 最末一格 */
    public static final int LAST_STAGE = STAGE_COUNT - 1;

    /** 格名，格编号即下标 */
    private static final String[] STAGE_NAMES = {"形式核验", "专家评议", "社会公示", "列入名录"};

    /**
     * 四家门类各按各的排法：门类只在这四家之内认，先后次序也按这一排法点，
     * 不许拿别家的名目凑数。
     */
    private static final String[] CATEGORIES = {"民间文学", "传统技艺", "传统医药", "传统音乐"};

    /** 收口说法：列入 */
    public static final int CLOSE_LISTED = 1;

    /** 收口说法：注销 */
    public static final int CLOSE_CANCELLED = 2;

    /** 收口说法：终止 */
    public static final int CLOSE_TERMINATED = 3;

    /** 一刻（一日）折毫秒，公示几日按整日掐 */
    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    private ItemFlowRuler() {
    }

    public static String stageName(int stage) {
        return stage >= 0 && stage < STAGE_COUNT ? STAGE_NAMES[stage] : "未知格";
    }

    public static String closeOutcomeName(Integer outcome) {
        if (outcome == null) {
            return "未收口";
        }
        if (outcome == CLOSE_LISTED) {
            return "列入";
        }
        if (outcome == CLOSE_CANCELLED) {
            return "注销";
        }
        if (outcome == CLOSE_TERMINATED) {
            return "终止";
        }
        return "未知说法";
    }

    /** 门类名目按固定排法给全（册子、页面同一处取） */
    public static List<String> categories() {
        List<String> all = new ArrayList<>();
        Collections.addAll(all, CATEGORIES);
        return all;
    }

    /** 这一门类排第几（0 起）；不在四家之内回 -1 */
    public static int categoryOrder(String category) {
        if (category == null) {
            return -1;
        }
        String c = category.trim();
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(c)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isValidCategory(String category) {
        return categoryOrder(category) >= 0;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * 一格门槛的回话：过没过、没过差哪几样（逐样点名，不许只回一句没通过）。
     */
    public static final class Gate {
        private final int stage;
        private final boolean passed;
        private final List<String> missing;

        Gate(int stage, boolean passed, List<String> missing) {
            this.stage = stage;
            this.passed = passed;
            this.missing = missing;
        }

        public int getStage() {
            return stage;
        }

        public String getStageName() {
            return ItemFlowRuler.stageName(stage);
        }

        public boolean isPassed() {
            return passed;
        }

        public List<String> getMissing() {
            return missing;
        }
    }

    /**
     * 点某一格的门槛过没过。{@code at} 为推进那一刻；公示格按它掐日子走完没走完。
     */
    public static Gate gateOf(int stage, TIchItemFlow bill, java.util.Date at) {
        List<String> missing = new ArrayList<>();
        if (bill == null) {
            missing.add("单据空着");
            return new Gate(stage, false, missing);
        }
        switch (stage) {
            case STAGE_VERIFY:
                // 形式核验四样齐，门类还得落在四家之内
                if (!notBlank(bill.getItemName())) {
                    missing.add("项目名称缺着");
                }
                if (!isValidCategory(bill.getCategory())) {
                    missing.add("门类不在民间文学/传统技艺/传统医药/传统音乐四家之内");
                }
                if (!notBlank(bill.getApplyArea())) {
                    missing.add("申报地缺着");
                }
                if (!notBlank(bill.getProtectUnit())) {
                    missing.add("保护单位缺着");
                }
                break;
            case STAGE_REVIEW:
                // 评议只认专家意见收齐这一条
                if (bill.getExpertOk() == null || bill.getExpertOk() != 1) {
                    missing.add("专家意见没收齐");
                }
                break;
            case STAGE_PUBLIC:
                // 公示守着当地定的几日；日子没走完，材料递得再齐也不算
                if (bill.getPublicDays() == null || bill.getPublicDays() <= 0) {
                    missing.add("当地定的公示几日没定");
                }
                if (bill.getPublicStart() == null) {
                    missing.add("公示还没起笔");
                } else if (at == null) {
                    missing.add("推进那一刻没注明，掐不出公示日子");
                } else if (!publicityElapsed(bill, at)) {
                    missing.add("公示日子没走完（定了" + bill.getPublicDays() + "日，还差"
                            + remainDays(bill, at) + "日）");
                }
                break;
            case STAGE_LIST:
                // 列入只看名录会议认不认
                if (bill.getMeetingOk() == null || bill.getMeetingOk() != 1) {
                    missing.add("名录会议没认");
                }
                break;
            default:
                missing.add("格编号越界: " + stage);
        }
        return new Gate(stage, missing.isEmpty(), missing);
    }

    /** 公示日子到 at 那一刻走没走完（整日掐，踩点那日算走完） */
    public static boolean publicityElapsed(TIchItemFlow bill, java.util.Date at) {
        if (bill == null || bill.getPublicStart() == null
                || bill.getPublicDays() == null || bill.getPublicDays() <= 0 || at == null) {
            return false;
        }
        long elapsed = at.getTime() - bill.getPublicStart().getTime();
        return elapsed >= bill.getPublicDays() * MILLIS_PER_DAY;
    }

    /** 还差几日走完（已走完回 0） */
    public static int remainDays(TIchItemFlow bill, java.util.Date at) {
        if (bill == null || bill.getPublicStart() == null
                || bill.getPublicDays() == null || bill.getPublicDays() <= 0 || at == null) {
            return 0;
        }
        long elapsedDays = (at.getTime() - bill.getPublicStart().getTime()) / MILLIS_PER_DAY;
        int remain = bill.getPublicDays() - (int) elapsedDays;
        return Math.max(0, remain);
    }

    /**
     * 顺着留痕点当前格：自核验起，第一格没有"现行已过口留痕"（pass_flag=1 且未被后退压下）
     * 的就是身处之格；四格都已过口时停在列入格。纸上格号、接口格号一概不取。
     */
    public static int currentStage(List<TIchItemFlowRecord> records) {
        boolean[] passed = new boolean[STAGE_COUNT];
        if (records != null) {
            for (TIchItemFlowRecord r : records) {
                if (r.getStage() == null || r.getStage() < 0 || r.getStage() >= STAGE_COUNT) {
                    continue;
                }
                boolean active = r.getPressed() == null || r.getPressed() == 0;
                boolean gone = r.getPassFlag() != null && r.getPassFlag() == 1;
                if (active && gone) {
                    passed[r.getStage()] = true;
                } else if (!active) {
                    // 被后退压到下面：先前过口不算数，从这格再从头攒
                    passed[r.getStage()] = false;
                }
            }
        }
        for (int s = 0; s < STAGE_COUNT; s++) {
            if (!passed[s]) {
                return s;
            }
        }
        return LAST_STAGE;
    }

    /** 该格此刻卷面在报（未过口、未压下）的那一笔；没有回 null */
    public static TIchItemFlowRecord pendingRecord(List<TIchItemFlowRecord> records, int stage) {
        TIchItemFlowRecord hit = null;
        for (TIchItemFlowRecord r : sorted(records)) {
            if (r.getStage() != null && r.getStage() == stage
                    && (r.getPressed() == null || r.getPressed() == 0)
                    && (r.getPassFlag() == null || r.getPassFlag() == 0)) {
                hit = r;
            }
        }
        return hit;
    }

    /** 该格此刻现行已过口（未压下）的那一笔；没有回 null */
    public static TIchItemFlowRecord passedRecord(List<TIchItemFlowRecord> records, int stage) {
        TIchItemFlowRecord hit = null;
        for (TIchItemFlowRecord r : sorted(records)) {
            if (r.getStage() != null && r.getStage() == stage
                    && (r.getPressed() == null || r.getPressed() == 0)
                    && r.getPassFlag() != null && r.getPassFlag() == 1) {
                hit = r;
            }
        }
        return hit;
    }

    /** 某一格重走到第几轮了：该格已留几笔（含压到下面的），新一笔轮次即此数（0 头一回起） */
    public static int nextRoundOf(List<TIchItemFlowRecord> records, int stage) {
        int n = 0;
        if (records != null) {
            for (TIchItemFlowRecord r : records) {
                if (r.getStage() != null && r.getStage() == stage) {
                    n++;
                }
            }
        }
        return n;
    }

    /** 按落格时刻、主键排好（空时刻靠后） */
    public static List<TIchItemFlowRecord> sorted(List<TIchItemFlowRecord> records) {
        List<TIchItemFlowRecord> ordered = new ArrayList<>(
                records == null ? Collections.emptyList() : records);
        ordered.sort(Comparator
                .comparing(TIchItemFlowRecord::getEnterTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(r -> r.getId() == null ? 0L : r.getId()));
        return ordered;
    }

    /**
     * 列入校验码：列入那一刻把单号、归口号、版次、四样、门类排法一并折进去。
     * 版次在码里，故一改版重算必为新码，新旧两版显出同一个码便是错的。
     * 纯算、不掺时刻随机，同料同回必同码——复算钉死的旧码也走这一处。
     */
    public static String entryCode(String bizNo, String declareNo, int versionNo,
                                   String itemName, String category, String applyArea,
                                   String protectUnit, java.util.Date listedTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("ICH|").append(trim(bizNo)).append('|').append(trim(declareNo))
                .append('|').append(versionNo)
                .append('|').append(trim(itemName))
                .append('|').append(trim(category)).append('[').append(categoryOrder(category)).append(']')
                .append('|').append(trim(applyArea))
                .append('|').append(trim(protectUnit))
                .append('|').append(listedTime == null ? "" : String.valueOf(listedTime.getTime()));
        return "JM" + MD5Util.encode(sb.toString()).toUpperCase();
    }

    /** 两版校验码显成同一个码便是错的（不同版次必不同码） */
    public static boolean versionsConflict(String codeV1, String codeV2) {
        return codeV1 != null && codeV1.equals(codeV2);
    }

    /**
     * 在册项目数两处对得齐才算：逐格点出的现行列入版数，与底册当场点出的在册数须同一个数。
     * 两个数须由调用方在同一回装载里点出，不许分两回取了再凑。
     */
    public static boolean rosterConsistent(int currentListedVersionCount, int activeProjectCount) {
        return currentListedVersionCount == activeProjectCount;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
