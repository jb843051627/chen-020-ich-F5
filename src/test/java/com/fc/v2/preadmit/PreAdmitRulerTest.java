package com.fc.v2.preadmit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TIchPreLine;
import com.fc.v2.model.custom.preadmit.DeclareVerdict;
import com.fc.v2.model.custom.preadmit.HeadJudge;
import com.fc.v2.model.custom.preadmit.PreAdmitRuler;
import com.fc.v2.model.custom.preadmit.PreDeclareInput;
import com.fc.v2.model.custom.preadmit.PreLineSaveReceipt;

/**
 * 申报准入唯一一把尺的规矩测试（纯算，不碰库）。
 *
 * <p>盯的规矩：半开区间与交替日、交棒日空按在场、顺位列长选线、
 * 四档边界、两头合判、无结论不凑数、立线逐栏点名。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class PreAdmitRulerTest {

    private static Date dt(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TIchPreLine line(long id, String code, int priority, Integer status,
                                    String start, String end) {
        TIchPreLine r = new TIchPreLine();
        r.setId(id);
        r.setRuleCode(code);
        r.setRuleName(code + "号线");
        r.setTh1Max(new BigDecimal("10"));
        r.setTh2Max(new BigDecimal("30"));
        r.setTh3Max(new BigDecimal("50"));
        r.setEffStart(start == null ? null : dt(start));
        r.setEffEnd(end == null ? null : dt(end));
        r.setPriority(priority);
        r.setStatus(status);
        r.setDelFlag(0);
        return r;
    }

    @Test
    public void 在场区间含启用日不含交棒日() {
        TIchPreLine r = line(1, "A", 1, 0, "2026-01-01 00:00:00", "2026-02-01 00:00:00");
        assertTrue(PreAdmitRuler.activeAt(r, dt("2026-01-01 00:00:00")));
        assertTrue(PreAdmitRuler.activeAt(r, dt("2026-01-15 12:00:00")));
        // 交棒当日（不含）已出去
        assertFalse(PreAdmitRuler.activeAt(r, dt("2026-02-01 00:00:00")));
        assertFalse(PreAdmitRuler.activeAt(r, dt("2025-12-31 23:59:59")));
    }

    @Test
    public void 交棒日空着按一直在场() {
        TIchPreLine r = line(1, "A", 1, 0, "2020-01-01 00:00:00", null);
        assertTrue(PreAdmitRuler.activeAt(r, dt("2026-10-03 00:00:00")));
        assertTrue(PreAdmitRuler.activeAt(r, dt("2099-01-01 00:00:00")));
        assertFalse(PreAdmitRuler.activeAt(r, dt("2019-12-31 23:59:59")));
    }

    @Test
    public void 停用与已删不进场() {
        TIchPreLine stopped = line(1, "A", 1, 1, null, null);
        TIchPreLine deleted = line(2, "B", 1, 0, null, null);
        deleted.setDelFlag(1);
        assertTrue(PreAdmitRuler.presentAt(Arrays.asList(stopped, deleted),
                dt("2026-10-03 00:00:00")).isEmpty());
    }

    @Test
    public void 交替日旧的出去新的进来同一刻两处同答() {
        TIchPreLine old = line(1, "OLD", 1, 0, "2025-01-01 00:00:00", "2026-01-01 00:00:00");
        TIchPreLine neu = line(2, "NEW", 1, 0, "2026-01-01 00:00:00", null);
        Date switchDay = dt("2026-01-01 00:00:00");
        // 交替当日：只有新线在场
        List<TIchPreLine> atSwitch = PreAdmitRuler.presentAt(Arrays.asList(old, neu), switchDay);
        assertEquals(1, atSwitch.size());
        assertEquals("NEW", atSwitch.get(0).getRuleCode());
        // 同一刻问两次（名单 / 定位），同一句答案
        assertEquals("NEW", PreAdmitRuler.topOf(atSwitch).getRuleCode());
        // 交替前一刻：只有旧线
        List<TIchPreLine> before = PreAdmitRuler.presentAt(Arrays.asList(old, neu),
                dt("2025-12-31 23:59:59"));
        assertEquals("OLD", before.get(0).getRuleCode());
    }

    @Test
    public void 让位顺位高的说话() {
        TIchPreLine low = line(1, "LOW", 1, 0, null, null);
        TIchPreLine high = line(2, "HIGH", 9, 0, null, null);
        high.setTh1Max(new BigDecimal("5"));
        high.setTh2Max(new BigDecimal("20"));
        high.setTh3Max(new BigDecimal("40"));
        List<TIchPreLine> present = PreAdmitRuler.presentAt(
                Arrays.asList(low, high), dt("2026-10-03 00:00:00"));
        // 8 对 HIGH 落过线层(3)，对 LOW 落起分层(1)；高顺位说话
        HeadJudge h = PreAdmitRuler.judgeHead(PreAdmitRuler.HEAD_YEARS, new BigDecimal("8"), present);
        assertEquals("HIGH", h.getRuleCode());
        assertEquals(PreAdmitRuler.TIER_PASS, h.getTier());
    }

    @Test
    public void 同顺位代号字数靠后的赢() {
        TIchPreLine ab = line(1, "AB", 1, 0, null, null);
        TIchPreLine abc = line(2, "ABC", 1, 0, null, null);
        abc.setTh1Max(new BigDecimal("5"));
        abc.setTh2Max(new BigDecimal("20"));
        abc.setTh3Max(new BigDecimal("40"));
        List<TIchPreLine> present = PreAdmitRuler.presentAt(
                Arrays.asList(ab, abc), dt("2026-10-03 00:00:00"));
        assertEquals("ABC", present.get(0).getRuleCode());
        // 回两次同一答案
        assertEquals(present.get(0), PreAdmitRuler.topOf(
                PreAdmitRuler.presentAt(Arrays.asList(ab, abc), dt("2026-10-03 00:00:00"))));
    }

    @Test
    public void 四档边界等号取本层() {
        TIchPreLine r = line(1, "A", 1, 0, null, null);
        assertEquals(PreAdmitRuler.TIER_START, PreAdmitRuler.tierOf(r, new BigDecimal("10")));
        assertEquals(PreAdmitRuler.TIER_PASS, PreAdmitRuler.tierOf(r, new BigDecimal("10.01")));
        assertEquals(PreAdmitRuler.TIER_PASS, PreAdmitRuler.tierOf(r, new BigDecimal("30")));
        assertEquals(PreAdmitRuler.TIER_EXCELLENT, PreAdmitRuler.tierOf(r, new BigDecimal("30.01")));
        assertEquals(PreAdmitRuler.TIER_EXCELLENT, PreAdmitRuler.tierOf(r, new BigDecimal("50")));
        assertEquals(PreAdmitRuler.TIER_OVER, PreAdmitRuler.tierOf(r, new BigDecimal("50.01")));
    }

    @Test
    public void 够不着任何一条时无结论且两栏空着() {
        Date at = dt("2026-10-03 00:00:00");
        PreDeclareInput in = new PreDeclareInput("P1", at, new BigDecimal("20"), new BigDecimal("40"));
        DeclareVerdict v = PreAdmitRuler.declare(in, PreAdmitRuler.presentAt(new ArrayList<>(), at));
        assertEquals(DeclareVerdict.VERDICT_NONE, v.getVerdict());
        assertEquals(2, v.getHeads().size());
        for (HeadJudge h : v.getHeads()) {
            assertFalse(h.isAvailable());
            assertNull(h.getRuleCode());
            assertNull(h.getEffStart());
            assertNull(h.getTier());
            assertNull(h.getPassed());
        }
    }

    @Test
    public void 两头都在过线与优线层才算准入() {
        TIchPreLine r = line(1, "A", 1, 0, null, null);
        List<TIchPreLine> present = PreAdmitRuler.presentAt(Arrays.asList(r), dt("2026-10-03 00:00:00"));
        DeclareVerdict pass = PreAdmitRuler.declare(
                new PreDeclareInput("P1", dt("2026-10-03 00:00:00"),
                        new BigDecimal("30"), new BigDecimal("50")), present);
        assertEquals(DeclareVerdict.VERDICT_PASS, pass.getVerdict());
    }

    @Test
    public void 一头起分层没过亮出该头的数() {
        TIchPreLine r = line(1, "A", 1, 0, null, null);
        List<TIchPreLine> present = PreAdmitRuler.presentAt(Arrays.asList(r), dt("2026-10-03 00:00:00"));
        DeclareVerdict v = PreAdmitRuler.declare(
                new PreDeclareInput("P1", dt("2026-10-03 00:00:00"),
                        new BigDecimal("5"), new BigDecimal("40")), present);
        assertEquals(DeclareVerdict.VERDICT_REJECT, v.getVerdict());
        HeadJudge years = v.head(PreAdmitRuler.HEAD_YEARS);
        assertFalse(years.getPassed());
        // 亮实际数与过线分界，不许只回一句没通过
        assertTrue(years.getPrompt().contains("5"));
        assertTrue(years.getPrompt().contains("30"));
        assertTrue(years.getPrompt().contains("从艺年数"));
        assertNull(v.head(PreAdmitRuler.HEAD_APPRENTICES).getPrompt());
    }

    @Test
    public void 一头冲过优线也不准入() {
        TIchPreLine r = line(1, "A", 1, 0, null, null);
        List<TIchPreLine> present = PreAdmitRuler.presentAt(Arrays.asList(r), dt("2026-10-03 00:00:00"));
        DeclareVerdict v = PreAdmitRuler.declare(
                new PreDeclareInput("P1", dt("2026-10-03 00:00:00"),
                        new BigDecimal("20"), new BigDecimal("99")), present);
        assertEquals(DeclareVerdict.VERDICT_REJECT, v.getVerdict());
        HeadJudge app = v.head(PreAdmitRuler.HEAD_APPRENTICES);
        assertEquals(PreAdmitRuler.TIER_OVER, app.getTier());
        assertTrue(app.getPrompt().contains("99"));
        assertTrue(app.getPrompt().contains("50"));
    }

    @Test
    public void 立线逐栏点名缺栏与分界不像话() {
        TIchPreLine form = new TIchPreLine();
        // 全空：线名、代号、三条分界逐一点名
        List<PreLineSaveReceipt.FieldError> errors = PreAdmitRuler.validateLine(form);
        List<String> fields = new ArrayList<>();
        for (PreLineSaveReceipt.FieldError e : errors) {
            fields.add(e.getField());
        }
        assertTrue(fields.contains("ruleName"));
        assertTrue(fields.contains("ruleCode"));
        assertTrue(fields.contains("th1Max"));
        assertTrue(fields.contains("th2Max"));
        assertTrue(fields.contains("th3Max"));
        // 两栏日子与让位顺位空着不点名：日子空按一直在场、顺位空按末位
        assertFalse(fields.contains("effStart"));
        assertFalse(fields.contains("effEnd"));
        assertFalse(fields.contains("priority"));
        assertFalse(PreAdmitRuler.isStanding(form));

        // 三条分界齐了但不从低到高
        TIchPreLine bad = line(1, "A", 1, 0, "2026-01-01 00:00:00", null);
        bad.setTh1Max(new BigDecimal("30"));
        bad.setTh2Max(new BigDecimal("30"));
        bad.setTh3Max(new BigDecimal("10"));
        List<PreLineSaveReceipt.FieldError> badErrors = PreAdmitRuler.validateLine(bad);
        assertTrue(badErrors.stream().anyMatch(e -> "thresholds".equals(e.getField())));
        assertFalse(PreAdmitRuler.isStanding(bad));

        // 像样的一条（交棒日仍空着）立得起来
        TIchPreLine good = line(2, "B", 1, 0, "2026-01-01 00:00:00", null);
        assertTrue(PreAdmitRuler.validateLine(good).isEmpty());
        assertTrue(PreAdmitRuler.isStanding(good));
    }

    @Test
    public void 来历核对认代号加启用之日() {
        TIchPreLine r = line(1, "A", 1, 0, "2026-01-01 00:00:00", null);
        List<TIchPreLine> present = PreAdmitRuler.presentAt(Arrays.asList(r), dt("2026-10-03 00:00:00"));
        // 代号与启用之日都合得上
        assertEquals("A", PreAdmitRuler.findPresent(present, "A", dt("2026-01-01 00:00:00")).getRuleCode());
        // 代号合、启用之日对不上：来历不对
        assertNull(PreAdmitRuler.findPresent(present, "A", dt("2025-01-01 00:00:00")));
        // 代号查无
        assertNull(PreAdmitRuler.findPresent(present, "X", dt("2026-01-01 00:00:00")));
    }
}
