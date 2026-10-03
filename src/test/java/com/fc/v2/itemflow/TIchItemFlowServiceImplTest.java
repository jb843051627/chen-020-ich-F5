package com.fc.v2.itemflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fc.v2.mapper.auto.TIchItemFlowMapper;
import com.fc.v2.mapper.auto.TIchItemFlowRecordMapper;
import com.fc.v2.mapper.auto.TIchProjectMapper;
import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.auto.TIchItemFlowRecord;
import com.fc.v2.model.auto.TIchProject;
import com.fc.v2.model.custom.itemflow.ItemFlowCommand;
import com.fc.v2.model.custom.itemflow.ItemFlowException;
import com.fc.v2.model.custom.itemflow.ItemFlowLedger;
import com.fc.v2.model.custom.itemflow.ItemFlowView;
import com.fc.v2.service.impl.TIchItemFlowServiceImpl;

/**
 * 唯一推进口 TIchItemFlowServiceImpl 的规矩测试（Mapper 全打桩，不起 Spring、不碰库）。
 *
 * @author fuce
 * @date 2026-10-03
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TIchItemFlowServiceImplTest {

    @Mock
    private TIchItemFlowMapper flowMapper;
    @Mock
    private TIchItemFlowRecordMapper recordMapper;
    @Mock
    private TIchProjectMapper projectMapper;

    @InjectMocks
    private TIchItemFlowServiceImpl service;

    private TIchItemFlow row;
    private final List<TIchItemFlow> flows = new ArrayList<>();
    private final List<TIchItemFlowRecord> records = new ArrayList<>();
    private final List<TIchProject> projects = new ArrayList<>();
    private final AtomicInteger flowSeq = new AtomicInteger(100);
    private final AtomicInteger recSeq = new AtomicInteger(1);
    private int updateResult = 1;
    private int runningCount = 0;

    private static final long DAY = 86_400_000L;

    @BeforeEach
    public void setUp() {
        row = baseFlow(7L, 0);
        flows.clear();
        flows.add(row);
        records.clear();
        projects.clear();
        updateResult = 1;
        runningCount = 0;

        when(flowMapper.selectById(any())).thenAnswer(inv -> findFlow(inv.getArgument(0)));
        when(flowMapper.selectList(any())).thenAnswer(inv -> {
            Object w = inv.getArgument(0);
            Object declareNo = eqVal(w, "declare_no");
            Object currentFlag = eqVal(w, "current_flag");
            List<TIchItemFlow> out = new ArrayList<>();
            for (TIchItemFlow f : flows) {
                if (f.getDelFlag() != null && f.getDelFlag() != 0) {
                    continue;
                }
                if (declareNo != null && !declareNo.equals(f.getDeclareNo())) {
                    continue;
                }
                if (currentFlag != null && !currentFlag.equals(f.getCurrentFlag())) {
                    continue;
                }
                out.add(f);
            }
            return out;
        });
        when(flowMapper.selectCount(any())).thenAnswer(inv -> runningCount);
        when(recordMapper.selectList(any())).thenAnswer(inv -> {
            Object flowId = eqVal(inv.getArgument(0), "flow_id");
            List<TIchItemFlowRecord> out = new ArrayList<>();
            for (TIchItemFlowRecord r : records) {
                if (flowId == null || flowId.equals(r.getFlowId())) {
                    out.add(r);
                }
            }
            return out;
        });
        when(projectMapper.selectList(any())).thenAnswer(inv -> {
            List<TIchProject> out = new ArrayList<>();
            for (TIchProject p : projects) {
                if (p.getDelFlag() == null || p.getDelFlag() == 0) {
                    out.add(p);
                }
            }
            return out;
        });
        when(projectMapper.selectOne(any())).thenAnswer(inv -> {
            for (TIchProject p : projects) {
                if ((p.getDelFlag() == null || p.getDelFlag() == 0)
                        && p.getStatus() != null && p.getStatus() == 0) {
                    return p;
                }
            }
            return null;
        });

        when(recordMapper.insert(any())).thenAnswer(inv -> {
            TIchItemFlowRecord r = inv.getArgument(0);
            r.setId((long) recSeq.getAndIncrement());
            if (r.getDelFlag() == null) {
                r.setDelFlag(0);
            }
            records.add(r);
            return 1;
        });
        when(flowMapper.insert(any())).thenAnswer(inv -> {
            TIchItemFlow f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId((long) flowSeq.getAndIncrement());
            }
            if (f.getDelFlag() == null) {
                f.setDelFlag(0);
            }
            flows.add(f);
            return 1;
        });
        when(projectMapper.insert(any())).thenAnswer(inv -> {
            TIchProject p = inv.getArgument(0);
            p.setId((long) flowSeq.getAndIncrement());
            projects.add(p);
            return 1;
        });
        when(projectMapper.updateById(any())).thenAnswer(inv -> 1);
        when(flowMapper.update(any(), any())).thenAnswer(inv -> {
            if (updateResult == 0) {
                return 0;
            }
            TIchItemFlow patch = inv.getArgument(0);
            for (TIchItemFlow f : flows) {
                applyPatch(f, patch);
            }
            return 1;
        });
    }

    private TIchItemFlow baseFlow(Long id) {
        TIchItemFlow f = new TIchItemFlow();
        f.setId(id);
        f.setBizNo("SB-2026-" + id);
        f.setDeclareNo("SB-2026-" + id);
        f.setVersionNo(0);
        f.setCurrentFlag(1);
        f.setRoundNo(0);
        f.setStage(0);
        f.setStatus(0);
        f.setCloseType(0);
        f.setItemName("苗绣");
        f.setSiteType("传统技艺");
        f.setApplyArea("澜川县");
        f.setProtectUnit("文化馆");
        f.setPublicDays(5);
        f.setDelFlag(0);
        return f;
    }

    private TIchItemFlow baseFlow(long id, int stage) {
        return baseFlow(id);
    }

    private void applyPatch(TIchItemFlow target, TIchItemFlow p) {
        if (p.getStage() != null) {
            target.setStage(p.getStage());
        }
        if (p.getRoundNo() != null) {
            target.setRoundNo(p.getRoundNo());
        }
        if (p.getStatus() != null) {
            target.setStatus(p.getStatus());
        }
        if (p.getCloseType() != null) {
            target.setCloseType(p.getCloseType());
        }
        if (p.getCheckCode() != null) {
            target.setCheckCode(p.getCheckCode());
        }
        if (p.getListedTime() != null) {
            target.setListedTime(p.getListedTime());
        }
        if (p.getSiteNo() != null) {
            target.setSiteNo(p.getSiteNo());
        }
        if (p.getPublicStart() != null) {
            target.setPublicStart(p.getPublicStart());
        }
        if (p.getLastAction() != null) {
            target.setLastAction(p.getLastAction());
        }
        if (p.getCurrentFlag() != null) {
            target.setCurrentFlag(p.getCurrentFlag());
        }
        if (p.getContent() != null) {
            target.setContent(p.getContent());
        }
        if (p.getDelFlag() != null) {
            target.setDelFlag(p.getDelFlag());
        }
    }

    private ItemFlowCommand command(String declareNo) {
        ItemFlowCommand c = new ItemFlowCommand();
        c.setDeclareNo(declareNo);
        c.setItemName("苗绣");
        c.setSiteType("传统技艺");
        c.setApplyArea("澜川县");
        c.setProtectUnit("文化馆");
        c.setPublicDays(5);
        return c;
    }

    private TIchProject project(String siteNo, int status) {
        TIchProject p = new TIchProject();
        p.setSiteNo(siteNo);
        p.setSiteName("苗绣");
        p.setSiteType("传统技艺");
        p.setStatus(status);
        p.setDelFlag(0);
        projects.add(p);
        return p;
    }

    /** 一路推进到列入，回每一格的 view，便于断言 */
    private ItemFlowView runToListed(Date t0) {
        service.move(7L, true, 0, "形式齐", t0);                 // 0->1
        row.setExpertsReceived(1);
        service.move(7L, true, 1, "专家齐", t0);               // 1->2 起算公示
        service.move(7L, true, 2, "公示满", new Date(t0.getTime() + 5 * DAY)); // 2->3
        row.setMeetingRecognized(1);
        return service.move(7L, true, 3, "会议认", new Date(t0.getTime() + 5 * DAY)); // 列入
    }

    @Test
    public void 老查询方法收什么给什么原样不动() {
        assertSame(row, service.selectTIchItemFlowById(7L));
    }

    @Test
    public void 查无此单抛未找到() {
        ItemFlowException ex = assertThrows(ItemFlowException.class, () -> service.view(999L));
        assertEquals(ItemFlowException.Reason.NOT_FOUND, ex.getReason());
    }

    @Test
    public void 开单落在形式核验格四样随单() {
        flows.removeIf(f -> f.getId().equals(7L));
        ItemFlowView v = service.declare(command("SB-NEW"));
        assertEquals(0, v.getCurrentStage());
        assertEquals(0, v.getStatus());
        assertEquals(1, v.getCurrentFlag());
    }

    @Test
    public void 同一份申报有在跑单后一张立不住() {
        runningCount = 1;
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.declare(command("SB-2026-7")));
        assertEquals(ItemFlowException.Reason.ALREADY_RUNNING, ex.getReason());
        verify(flowMapper, never()).insert(any());
    }

    @Test
    public void 门类不在四样排法开单被挡() {
        ItemFlowCommand c = command("SB-X");
        c.setSiteType("传统舞蹈");
        ItemFlowException ex = assertThrows(ItemFlowException.class, () -> service.declare(c));
        assertEquals(ItemFlowException.Reason.BAD_CATEGORY, ex.getReason());
    }

    @Test
    public void 一路四格门槛过了才列入列入钉码入册() {
        Date t0 = new Date(2_000_000L);
        ItemFlowView listed = runToListed(t0);
        assertEquals(3, listed.getCurrentStage());
        assertEquals(2, listed.getStatus());
        assertTrue(listed.isLocked());
        assertEquals(1, listed.getCloseType());
        assertNotNull(row.getCheckCode());
        assertNotNull(row.getListedTime());
        // 列入即在册数 +1，底册造了一条在册项目
        assertEquals(1, projects.size());
        assertEquals(0, projects.get(0).getStatus());

        ItemFlowLedger ledger = service.ledger();
        assertTrue(ledger.isCountMatched());
        assertEquals(1, ledger.getFlowRegisteredCount());
        assertEquals(1, ledger.getRegistryRegisteredCount());
    }

    @Test
    public void 专家没齐评议门槛挡着不进公示() {
        Date t0 = new Date(3_000_000L);
        service.move(7L, true, 0, "形式齐", t0);
        assertEquals(1, row.getStage());
        int before = records.size();
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.move(7L, true, 1, "专家其实没齐", t0));
        assertEquals(ItemFlowException.Reason.GATE_NOT_MET, ex.getReason());
        assertEquals(before, records.size());
        assertEquals(1, row.getStage());
    }

    @Test
    public void 公示日子没走完材料再齐也不能进列入格() {
        Date t0 = new Date(4_000_000L);
        service.move(7L, true, 0, null, t0);
        row.setExpertsReceived(1);
        service.move(7L, true, 1, null, t0); // 进公示，起算
        assertEquals(2, row.getStage());
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.move(7L, true, 2, null, new Date(t0.getTime() + 2 * DAY)));
        assertEquals(ItemFlowException.Reason.GATE_NOT_MET, ex.getReason());
        assertEquals(2, row.getStage());
    }

    @Test
    public void 意向格号与回算相左按入口的来不写第二笔() {
        // 单还在形式核验(0)，屏上却带列入格(3)
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.move(7L, true, 3, "越格", new Date()));
        assertEquals(ItemFlowException.Reason.STAGE_MISMATCH, ex.getReason());
        verify(recordMapper, never()).insert(any());
        assertEquals(0, row.getStage());

        // 推进过一次后再拿旧格号重点，被挡且不另起一行
        service.move(7L, true, 0, null, new Date());
        int lines = records.size();
        ItemFlowException again = assertThrows(ItemFlowException.class,
                () -> service.move(7L, true, 0, "重复点", new Date()));
        assertEquals(ItemFlowException.Reason.STAGE_MISMATCH, again.getReason());
        assertEquals(lines, records.size());
    }

    @Test
    public void 后退只退一格轮次加一旧笔不抹重走从该格攒() {
        Date t0 = new Date(5_000_000L);
        service.move(7L, true, 0, "核验头遍", t0); // ->1
        int archived = records.size();

        ItemFlowView back = service.move(7L, false, 1, "退回核验", t0);
        assertEquals(0, back.getCurrentStage());
        assertEquals(1, back.getRoundNo());
        assertEquals(0, row.getStage());
        assertEquals(1, row.getRoundNo());
        assertEquals(archived + 1, records.size()); // 只多一笔 BACK，旧 ADVANCE 不抹

        ItemFlowView again = service.view(7L);
        assertFalse(again.getStages().get(0).isPassed()); // 该格压到下面，从头攒
    }

    @Test
    public void 头一格无格可退() {
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.move(7L, false, 0, null, new Date()));
        assertEquals(ItemFlowException.Reason.NO_PREV_STAGE, ex.getReason());
    }

    @Test
    public void 收口当场锁档推进改字删除全被挡() {
        runToListed(new Date(6_000_000L));
        int linesAfterLock = records.size();

        ItemFlowException m = assertThrows(ItemFlowException.class,
                () -> service.move(7L, false, 3, null, new Date()));
        assertEquals(ItemFlowException.Reason.LOCKED, m.getReason());
        ItemFlowException c = assertThrows(ItemFlowException.class,
                () -> service.close(7L, 3, null, null, new Date()));
        assertEquals(ItemFlowException.Reason.LOCKED, c.getReason());
        assertFalse(service.updateContent(7L, "事后改字"));
        assertFalse(service.remove(7L));
        // 锁后再没有任何新留痕落笔
        assertEquals(linesAfterLock, records.size());
    }

    @Test
    public void 终止收口不入册也不减册() {
        ItemFlowView v = service.close(7L, 3, null, "中途终止", new Date());
        assertTrue(v.isLocked());
        assertEquals(3, v.getCloseType());
        assertEquals(0, projects.size());
        verify(projectMapper, never()).updateById(any());
    }

    @Test
    public void 注销须指在册项目当场翻注销在册数减一() {
        project("XM-9", 0);
        ItemFlowException noNo = assertThrows(ItemFlowException.class,
                () -> service.close(7L, 2, "  ", null, new Date()));
        assertEquals(ItemFlowException.Reason.BAD_ARGUMENT, noNo.getReason());

        ItemFlowView v = service.close(7L, 2, "XM-9", "注销", new Date());
        assertTrue(v.isLocked());
        assertEquals(2, v.getCloseType());
        assertEquals(1, projects.get(0).getStatus());

        // 一条列入的现行版 + 本条注销：收口单口径在册0，底册也0在册
        TIchItemFlow listed = baseFlow(200L);
        listed.setStatus(2);
        listed.setCloseType(1);
        listed.setCheckCode("c-200");
        flows.add(listed);
        projects.clear();
        ItemFlowLedger ledger = service.ledger();
        assertTrue(ledger.isCountMatched());
        assertEquals(0, ledger.getRegistryRegisteredCount());
    }

    @Test
    public void 注销所对项目不在册被挡() {
        project("XM-9", 1); // 已注销
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.close(7L, 2, "XM-9", null, new Date()));
        assertEquals(ItemFlowException.Reason.BAD_ARGUMENT, ex.getReason());
    }

    @Test
    public void 变更另起一版旧版转往期新版重算码两版不同码() {
        runToListed(new Date(7_000_000L));
        String oldCode = row.getCheckCode();

        ItemFlowCommand change = command(row.getDeclareNo());
        change.setProtectUnit("非遗保护中心");
        ItemFlowView nv = service.revise(7L, change);

        assertEquals(1, nv.getVersionNo());
        assertEquals(1, nv.getCurrentFlag());
        assertEquals(0, nv.getCurrentStage());
        assertEquals(0, nv.getStatus());
        assertNull(nv.getCheckCode()); // 新版尚未列入，码留到再列入重算
        assertEquals(0, row.getCurrentFlag()); // 旧版转往期

        // 新版再走一遍到列入，版次不同校验码必与旧版不同
        Long newId = Long.valueOf(nv.getFlowId());
        Date t0 = new Date(8_000_000L);
        service.move(newId, true, 0, null, t0);
        TIchItemFlow nrow = findFlow(newId);
        nrow.setExpertsReceived(1);
        service.move(newId, true, 1, null, t0);
        service.move(newId, true, 2, null, new Date(t0.getTime() + 5 * DAY));
        nrow.setMeetingRecognized(1);
        ItemFlowView relisted = service.move(newId, true, 3, null, new Date(t0.getTime() + 5 * DAY));
        assertTrue(relisted.isLocked());
        assertNotEquals(oldCode, relisted.getCheckCode());
    }

    @Test
    public void 未列入的现行版不许另起一版() {
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.revise(7L, command("SB-2026-7")));
        assertEquals(ItemFlowException.Reason.NOT_LISTED, ex.getReason());
    }

    @Test
    public void 落笔时被别人推动报并发变更() {
        updateResult = 0;
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.move(7L, true, 0, null, new Date()));
        assertEquals(ItemFlowException.Reason.CONCURRENT_CHANGED, ex.getReason());
    }

    @Test
    public void 未收口可改字可逻辑删除() {
        assertTrue(service.updateContent(7L, "补一笔卷面"));
        assertEquals("补一笔卷面", row.getContent());
        assertTrue(service.remove(7L));
        assertEquals(1, row.getDelFlag().intValue());
    }

    private TIchItemFlow findFlow(Long id) {
        for (TIchItemFlow f : flows) {
            if (f.getId() != null && f.getId().equals(id)) {
                return f;
            }
        }
        return null;
    }

    /** 从 MyBatis-Plus 条件里取 column = ? 的实参（测试打桩用，取不到返回 null）。 */
    @SuppressWarnings("unchecked")
    private static Object eqVal(Object wrapper, String column) {
        try {
            java.lang.reflect.Method sql = wrapper.getClass().getMethod("getSqlSegment");
            String seg = String.valueOf(sql.invoke(wrapper));
            java.lang.reflect.Method pm = wrapper.getClass().getMethod("getParamNameValuePairs");
            java.util.Map<String, Object> params =
                    (java.util.Map<String, Object>) pm.invoke(wrapper);
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile(column + "\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.([A-Za-z0-9]+)\\}")
                    .matcher(seg);
            if (m.find()) {
                return params.get(m.group(1));
            }
        } catch (Exception ignore) {
            // 打桩取不到条件就退化为不过滤
        }
        return null;
    }
}
