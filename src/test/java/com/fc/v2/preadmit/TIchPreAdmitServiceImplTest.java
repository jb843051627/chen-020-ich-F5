package com.fc.v2.preadmit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fc.v2.mapper.auto.TIchPreDeclareHeadMapper;
import com.fc.v2.mapper.auto.TIchPreDeclareMapper;
import com.fc.v2.mapper.auto.TIchPreLineMapper;
import com.fc.v2.model.auto.TIchPreDeclare;
import com.fc.v2.model.auto.TIchPreDeclareHead;
import com.fc.v2.model.auto.TIchPreLine;
import com.fc.v2.model.custom.preadmit.DeclareVerdict;
import com.fc.v2.model.custom.preadmit.HeadJudge;
import com.fc.v2.model.custom.preadmit.PreAdmitReport;
import com.fc.v2.model.custom.preadmit.PreDeclareInput;
import com.fc.v2.model.custom.preadmit.PreLineSaveReceipt;
import com.fc.v2.model.custom.preadmit.StaleHeadRef;
import com.fc.v2.service.impl.TIchPreAdmitServiceImpl;

/**
 * 申报准入唯一判据出口的规矩测试（Mapper 全打桩，不碰库）。
 *
 * <p>盯旧毛病：在场名单与逐条判定两回取数、复算拿最新线顶替、无结论凑邻近线、
 * 停用线仍进场、旧案来历看不出、落库字段不依出口回话。
 *
 * @author fuce
 * @date 2026-10-03
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TIchPreAdmitServiceImplTest {

    @Mock
    private TIchPreLineMapper preLineMapper;

    @Mock
    private TIchPreDeclareMapper declareMapper;

    @Mock
    private TIchPreDeclareHeadMapper headMapper;

    @InjectMocks
    private TIchPreAdmitServiceImpl service;

    private final List<TIchPreLine> lines = new ArrayList<>();
    private final List<TIchPreDeclare> declares = new ArrayList<>();
    private final List<TIchPreDeclareHead> headRows = new ArrayList<>();

    private static Date dt(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TIchPreLine line(long id, String code, int priority, Integer status,
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
        lines.add(r);
        return r;
    }

    private long declareSeq = 1000L;

    @BeforeEach
    public void setUp() {
        when(preLineMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(lines));
        when(declareMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(declares));
        when(headMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(headRows));
        when(declareMapper.selectById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            for (TIchPreDeclare d : declares) {
                if (d.getId().equals(id)) {
                    return d;
                }
            }
            return null;
        });
        when(declareMapper.insert(any(TIchPreDeclare.class))).thenAnswer(inv -> {
            TIchPreDeclare d = inv.getArgument(0);
            d.setId(++declareSeq);
            declares.add(d);
            return 1;
        });
        when(declareMapper.updateById(any())).thenReturn(1);
        when(preLineMapper.insert(any(TIchPreLine.class))).thenAnswer(inv -> {
            TIchPreLine r = inv.getArgument(0);
            r.setId(++declareSeq);
            lines.add(r);
            return 1;
        });
        when(preLineMapper.updateById(any())).thenReturn(1);
        when(headMapper.insert(any(TIchPreDeclareHead.class))).thenAnswer(inv -> {
            TIchPreDeclareHead h = inv.getArgument(0);
            h.setId(++declareSeq);
            headRows.add(h);
            return 1;
        });
    }

    @Test
    public void 复算拿申报自己的日子不拿最新线顶替() {
        line(1, "OLD", 1, 0, "2024-01-01 00:00:00", "2026-01-01 00:00:00");
        line(2, "NEW", 1, 0, "2026-01-01 00:00:00", null);
        DeclareVerdict v = service.judgeOne(new PreDeclareInput(
                "P1", dt("2025-06-01 00:00:00"), new BigDecimal("20"), new BigDecimal("40")));
        assertEquals("OLD", v.head(0).getRuleCode());
        assertEquals(dt("2024-01-01 00:00:00"), v.head(0).getEffStart());
    }

    @Test
    public void 批次名单与逐条同一回装载且各按各的日子() {
        line(1, "OLD", 1, 0, "2024-01-01 00:00:00", "2026-01-01 00:00:00");
        line(2, "NEW", 1, 0, "2026-01-01 00:00:00", null);

        List<PreDeclareInput> inputs = Arrays.asList(
                new PreDeclareInput("P1", dt("2025-06-01 00:00:00"), new BigDecimal("20"), new BigDecimal("40")),
                new PreDeclareInput("P2", dt("2026-05-01 00:00:00"), new BigDecimal("20"), new BigDecimal("40")),
                new PreDeclareInput("P3", dt("2023-06-01 00:00:00"), new BigDecimal("20"), new BigDecimal("40")));
        PreAdmitReport report = service.judgeBatch(inputs, dt("2026-10-03 00:00:00"));

        // 线册只装载这一回：名单与逐条同一快照
        verify(preLineMapper, times(1)).selectList(any());
        // 本轮开跑那一刻在场名单只有 NEW
        assertEquals(1, report.getPresentLines().size());
        assertEquals("NEW", report.getPresentLines().get(0).getRuleCode());
        // 逐条各按各的日子：P1 走 OLD，P2 走 NEW，P3 无结论
        assertEquals("OLD", report.getVerdicts().get(0).head(0).getRuleCode());
        assertEquals("NEW", report.getVerdicts().get(1).head(0).getRuleCode());
        assertEquals(DeclareVerdict.VERDICT_NONE, report.getVerdicts().get(2).getVerdict());
        // 三个数同一回点算
        assertEquals(3, report.getJudged());
        assertEquals(2, report.getAdmitted());
        assertEquals(0, report.getRejected());
        assertEquals(1, report.getNoConclusion());
    }

    @Test
    public void 无结论落库两栏空着不凑邻近线() {
        // 唯一一条线还没启用
        line(1, "FUTURE", 1, 0, "2030-01-01 00:00:00", null);
        DeclareVerdict v = service.judgeOne(new PreDeclareInput(
                "P1", dt("2026-10-03 00:00:00"), new BigDecimal("20"), new BigDecimal("40")));
        assertEquals(DeclareVerdict.VERDICT_NONE, v.getVerdict());

        TIchPreDeclare main = declares.get(0);
        assertEquals(0, main.getVerdict().intValue());
        assertEquals(2, main.getHeadTotal().intValue());
        ArgumentCaptor<TIchPreDeclareHead> captor = ArgumentCaptor.forClass(TIchPreDeclareHead.class);
        verify(headMapper, times(2)).insert(captor.capture());
        for (TIchPreDeclareHead h : captor.getAllValues()) {
            assertNull(h.getRuleCode());
            assertNull(h.getEffStart());
            assertNull(h.getTier());
            assertNull(h.getPassed());
        }
    }

    @Test
    public void 准入落库字段以出口回话钉死() {
        line(1, "A", 1, 0, "2026-01-01 00:00:00", null);
        service.judgeOne(new PreDeclareInput(
                "P1", dt("2026-05-01 00:00:00"), new BigDecimal("30"), new BigDecimal("50")));
        ArgumentCaptor<TIchPreDeclareHead> captor = ArgumentCaptor.forClass(TIchPreDeclareHead.class);
        verify(headMapper, times(2)).insert(captor.capture());
        for (TIchPreDeclareHead h : captor.getAllValues()) {
            assertEquals("A", h.getRuleCode());
            assertEquals(dt("2026-01-01 00:00:00"), h.getEffStart());
            assertEquals(1, h.getPassed().intValue());
        }
    }

    @Test
    public void 停用线不进场但在册翻得到() {
        line(1, "OFF", 9, 1, null, null);
        line(2, "ON", 1, 0, null, null);
        // 现行这一路拿的是 ON，停用的 OFF 顺位再高也不进场
        assertEquals("ON", service.locateAt(dt("2026-10-03 00:00:00")).getRuleCode());
        assertFalse(service.presentLines(dt("2026-10-03 00:00:00")).stream()
                .anyMatch(r -> "OFF".equals(r.getRuleCode())));
        // 按下不等于撕掉：原表里仍翻得到
        assertEquals(2, preLineMapper.selectList(null).size());
    }

    @Test
    public void 来历不对的旧案逐条列出() {
        // 现册：OLD 已按下停用，NEW 在场；GONE 整行不在册（del）
        line(1, "OLD", 1, 1, "2024-01-01 00:00:00", "2026-01-01 00:00:00");
        line(2, "NEW", 1, 0, "2026-01-01 00:00:00", null);
        seedDeclare(501L, "SB501", "P1", dt("2025-06-01 00:00:00"),
                "OLD", dt("2024-01-01 00:00:00"), new BigDecimal("20"),
                "OLD", dt("2024-01-01 00:00:00"), new BigDecimal("40"));
        seedDeclare(502L, "SB502", "P2", dt("2025-07-01 00:00:00"),
                "GONE", dt("2023-01-01 00:00:00"), new BigDecimal("20"),
                "GONE", dt("2023-01-01 00:00:00"), new BigDecimal("40"));
        seedDeclare(503L, "SB503", "P3", dt("2026-05-01 00:00:00"),
                "NEW", dt("2026-01-01 00:00:00"), new BigDecimal("20"),
                "NEW", dt("2026-01-01 00:00:00"), new BigDecimal("40"));
        seedNoConclusionDeclare(504L, "P4", dt("2026-05-01 00:00:00"));

        List<StaleHeadRef> stale = service.listStaleRefs();
        // 去年两份的四头来历不对；今年的与无结论的不列
        assertEquals(4, stale.size());
        assertTrue(stale.stream().anyMatch(s ->
                "SB501".equals(s.getDeclareNo())
                        && TIchPreAdmitServiceImpl.STALE_NOT_PRESENT.equals(s.getReason())));
        assertTrue(stale.stream().anyMatch(s ->
                "SB502".equals(s.getDeclareNo())
                        && TIchPreAdmitServiceImpl.STALE_CODE_MISSING.equals(s.getReason())
                        && "GONE".equals(s.getPinnedRuleCode())));
        assertFalse(stale.stream().anyMatch(s -> "SB503".equals(s.getDeclareNo())));
        assertFalse(stale.stream().anyMatch(s -> "P4".equals(s.getApplicantCode())));
    }

    @Test
    public void 旧档翻看只取钉死的字不按今线重判() {
        seedDeclare(601L, "SB601", "P1", dt("2025-06-01 00:00:00"),
                "OLD", dt("2024-01-01 00:00:00"), new BigDecimal("5"),
                "OLD", dt("2024-01-01 00:00:00"), new BigDecimal("40"));
        DeclareVerdict v = service.loadVerdict(601L);
        assertEquals(DeclareVerdict.VERDICT_REJECT, v.getVerdict());
        HeadJudge years = v.head(0);
        assertEquals("OLD", years.getRuleCode());
        assertNotNull(years.getPrompt());
        assertTrue(years.getPrompt().contains("5"));
    }

    @Test
    public void 立线逐栏校验与按下旧线() {
        PreLineSaveReceipt bad = service.appendLine(new TIchPreLine());
        assertFalse(bad.isSaved());
        // 线名、代号、三条分界逐一点名；两栏日子与顺位空着不算毛病
        assertTrue(bad.getErrors().size() >= 5);
        verify(preLineMapper, times(0)).insert(any(TIchPreLine.class));

        TIchPreLine form = new TIchPreLine();
        form.setRuleCode("C");
        form.setRuleName("C号线");
        form.setTh1Max(new BigDecimal("10"));
        form.setTh2Max(new BigDecimal("30"));
        form.setTh3Max(new BigDecimal("50"));
        form.setEffStart(dt("2026-10-03 00:00:00"));
        form.setPriority(1);
        PreLineSaveReceipt ok = service.appendLine(form);
        assertTrue(ok.isSaved());
        assertEquals(0, ok.getLine().getStatus().intValue());

        when(preLineMapper.updateById(any())).thenReturn(1);
        assertEquals(1, service.retireLine(form.getId()));
        ArgumentCaptor<TIchPreLine> captor = ArgumentCaptor.forClass(TIchPreLine.class);
        verify(preLineMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getStatus().intValue());
    }

    private void seedDeclare(long id, String no, String applicant, Date applyAt,
                             String code0, Date start0, BigDecimal m0,
                             String code1, Date start1, BigDecimal m1) {
        TIchPreDeclare d = new TIchPreDeclare();
        d.setId(id);
        d.setDeclareNo(no);
        d.setApplicantCode(applicant);
        d.setApplyAt(applyAt);
        d.setVerdict(DeclareVerdict.VERDICT_REJECT);
        d.setHeadTotal(2);
        d.setDelFlag(0);
        declares.add(d);
        headRows.add(head(7000L + id * 10, id, 0, "从艺年数", m0, code0, start0));
        headRows.add(head(7001L + id * 10, id, 1, "带徒人数", m1, code1, start1));
    }

    private void seedNoConclusionDeclare(long id, String applicant, Date applyAt) {
        TIchPreDeclare d = new TIchPreDeclare();
        d.setId(id);
        d.setDeclareNo("SB" + id);
        d.setApplicantCode(applicant);
        d.setApplyAt(applyAt);
        d.setVerdict(DeclareVerdict.VERDICT_NONE);
        d.setHeadTotal(2);
        d.setDelFlag(0);
        declares.add(d);
        headRows.add(head(7000L + id * 10, id, 0, "从艺年数", new BigDecimal("20"), null, null));
        headRows.add(head(7001L + id * 10, id, 1, "带徒人数", new BigDecimal("40"), null, null));
    }

    private TIchPreDeclareHead head(long hid, long declareId, int headNo, String name,
                                    BigDecimal metric, String code, Date effStart) {
        TIchPreDeclareHead h = new TIchPreDeclareHead();
        h.setId(hid);
        h.setDeclareId(declareId);
        h.setHeadNo(headNo);
        h.setHeadName(name);
        h.setMetric(metric);
        h.setRuleCode(code);
        h.setEffStart(effStart);
        if (code != null) {
            h.setTier(2);
            h.setPassed(headNo == 1 ? 1 : 0);
            if (headNo == 0) {
                h.setPrompt("从艺年数报 5，没够着过线 30");
            }
        }
        h.setDelFlag(0);
        return h;
    }
}
