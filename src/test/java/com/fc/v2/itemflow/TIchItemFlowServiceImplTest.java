package com.fc.v2.itemflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import com.fc.v2.service.impl.TIchItemFlowServiceImpl;

/**
 * 名录项目申报单四格流转的规矩测试（Mapper 全打桩，内存账本代库，不起 Spring）。
 *
 * <p>盯这几条：身处第几格靠点已过之格回算、前进只到挨着的格且门槛不过不收
 * （公示日子没走完不算）、同格二报不落第二笔、后退旧记压下重走从头攒、
 * 收口当场锁档、一份申报只容一张在跑、列入钉码落底册、变更旧版挪往期且新旧不重码、
 * 在册数两处对算同回。
 *
 * @author fuce
 * @date 2026-10-03
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TIchItemFlowServiceImplTest {

    @Mock
    private TIchItemFlowMapper billMapper;
    @Mock
    private TIchItemFlowRecordMapper recordMapper;
    @Mock
    private TIchItemFlowVersionMapper versionMapper;
    @Mock
    private TIchProjectMapper projectMapper;

    @InjectMocks
    private TIchItemFlowServiceImpl service;

    private final Map<Long, TIchItemFlow> bills = new LinkedHashMap<>();
    private final List<TIchItemFlowRecord> records = new ArrayList<>();
    private final List<TIchItemFlowVersion> versions = new ArrayList<>();
    private final List<TIchProject> projects = new ArrayList<>();
    private final AtomicLong idSeq = new AtomicLong(100);

    private static final long DAY = 24L * 60 * 60 * 1000;

    @BeforeEach
    public void setUp() {
        stubBills();
        stubRecords();
        stubVersions();
        stubProjects();
    }

    // ---------------------------------------------------------------- 打桩

    private void stubBills() {
        when(billMapper.selectById(any())).thenAnswer(inv -> bills.get(inv.getArgument(0)));
        when(billMapper.selectList(any())).thenAnswer(inv -> {
            List<TIchItemFlow> all = new ArrayList<>(bills.values());
            return filter(all, inv.getArgument(0));
        });
        when(billMapper.insert(any())).thenAnswer(inv -> {
            TIchItemFlow b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(idSeq.incrementAndGet());
            }
            if (b.getDelFlag() == null) {
                b.setDelFlag(0);
            }
            bills.put(b.getId(), b);
            return 1;
        });
        when(billMapper.update(any(), any())).thenAnswer(inv -> {
            TIchItemFlow patch = inv.getArgument(0);
            List<TIchItemFlow> hit = filter(new ArrayList<>(bills.values()), inv.getArgument(1));
            for (TIchItemFlow b : hit) {
                copyNonNull(patch, b);
            }
            return hit.size();
        });
        when(billMapper.updateById(any())).thenAnswer(inv -> {
            TIchItemFlow b = inv.getArgument(0);
            TIchItemFlow cur = bills.get(b.getId());
            if (cur == null) {
                return 0;
            }
            copyNonNull(b, cur);
            return 1;
        });
        when(billMapper.deleteById(any())).thenAnswer(inv -> bills.remove(inv.getArgument(0)) != null ? 1 : 0);
    }

    private void stubRecords() {
        when(recordMapper.selectList(any())).thenAnswer(inv ->
                filter(new ArrayList<>(records), inv.getArgument(0)));
        when(recordMapper.insert(any())).thenAnswer(inv -> {
            TIchItemFlowRecord r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(idSeq.incrementAndGet());
            }
            if (r.getDelFlag() == null) {
                r.setDelFlag(0);
            }
            records.add(r);
            return 1;
        });
        when(recordMapper.updateById(any())).thenAnswer(inv -> {
            TIchItemFlowRecord patch = inv.getArgument(0);
            for (TIchItemFlowRecord r : records) {
                if (r.getId().equals(patch.getId())) {
                    copyNonNull(patch, r);
                    return 1;
                }
            }
            return 0;
        });
    }

    private void stubVersions() {
        when(versionMapper.selectList(any())).thenAnswer(inv ->
                filter(new ArrayList<>(versions), inv.getArgument(0)));
        when(versionMapper.insert(any())).thenAnswer(inv -> {
            TIchItemFlowVersion v = inv.getArgument(0);
            if (v.getId() == null) {
                v.setId(idSeq.incrementAndGet());
            }
            if (v.getDelFlag() == null) {
                v.setDelFlag(0);
            }
            versions.add(v);
            return 1;
        });
        when(versionMapper.update(any(), any())).thenAnswer(inv -> {
            TIchItemFlowVersion patch = inv.getArgument(0);
            List<TIchItemFlowVersion> hit = filter(new ArrayList<>(versions), inv.getArgument(1));
            for (TIchItemFlowVersion v : hit) {
                copyNonNull(patch, v);
            }
            return hit.size();
        });
    }

    private void stubProjects() {
        when(projectMapper.selectList(any())).thenAnswer(inv ->
                filter(new ArrayList<>(projects), inv.getArgument(0)));
        when(projectMapper.insert(any())).thenAnswer(inv -> {
            TIchProject p = inv.getArgument(0);
            p.setId(idSeq.incrementAndGet());
            projects.add(p);
            return 1;
        });
        when(projectMapper.updateById(any())).thenAnswer(inv -> {
            TIchProject patch = inv.getArgument(0);
            for (TIchProject p : projects) {
                if (p.getId().equals(patch.getId())) {
                    copyNonNull(patch, p);
                    return 1;
                }
            }
            return 0;
        });
    }

    // ---------------------------------------------------------------- 造数

    private ItemFlowForm fullForm(String declareNo) {
        ItemFlowForm f = new ItemFlowForm();
        f.setDeclareNo(declareNo);
        f.setItemName("澜川苗绣");
        f.setCategory("传统技艺");
        f.setApplyArea("澜川县河湾区");
        f.setProtectUnit("澜川县文化馆");
        return f;
    }

    /** 从核验一路推进到列入，公示 t0 起 5 日，末推在 t0+5 日 */
    private TIchItemFlow runToListed(String declareNo, Date t0) {
        ItemFlowForm f = fullForm(declareNo);
        f.setPublicDays(5);
        f.setPublicStart(t0);
        ItemFlowView opened = service.open(f);
        Long id = Long.valueOf(opened.getBillId());
        service.report(id, expertForm(1));
        service.pushForward(id, 1, t0);              // 核验 → 评议
        service.pushForward(id, 2, t0);              // 评议 → 公示（专家已齐）
        service.report(id, meetingForm(1));
        // 公示日子没走完，后一格开着也不能进
        ItemFlowException early = assertThrows(ItemFlowException.class,
                () -> service.pushForward(id, 3, new Date(t0.getTime() + 4 * DAY)));
        assertEquals(ItemFlowException.Reason.GATE_NOT_MET, early.getReason());
        service.pushForward(id, 3, new Date(t0.getTime() + 5 * DAY)); // 公示 → 列入格
        service.pushForward(id, 3, new Date(t0.getTime() + 5 * DAY)); // 名录会议认下 → 列入收口
        return bills.get(id);
    }

    private ItemFlowForm expertForm(int ok) {
        ItemFlowForm f = new ItemFlowForm();
        f.setExpertOk(ok);
        return f;
    }

    private ItemFlowForm meetingForm(int ok) {
        ItemFlowForm f = new ItemFlowForm();
        f.setMeetingOk(ok);
        return f;
    }

    private long recordCountOfStage(long billId, int stage) {
        return records.stream().filter(r -> r.getBillId() == billId && r.getStage() != null
                && r.getStage() == stage).count();
    }

    // ---------------------------------------------------------------- 测试

    @Test
    public void 老查格次方法签名原样保留收什么给什么() {
        ItemFlowView v = service.open(fullForm("D-OLD"));
        TIchItemFlow raw = service.selectTIchItemFlowById(Long.valueOf(v.getBillId()));
        assertSame(bills.get(Long.valueOf(v.getBillId())), raw);
    }

    @Test
    public void 门类不在四家之内立不住单() {
        ItemFlowForm f = fullForm("D-CAT");
        f.setCategory("杂技");
        ItemFlowException ex = assertThrows(ItemFlowException.class, () -> service.open(f));
        assertEquals(ItemFlowException.Reason.BAD_CATEGORY, ex.getReason());
        assertTrue(bills.isEmpty());
    }

    @Test
    public void 一份申报只容一张在跑头一张没喊停后一张立不住() {
        service.open(fullForm("D-ONE"));
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.open(fullForm("D-ONE")));
        assertEquals(ItemFlowException.Reason.DUPLICATE_RUNNING, ex.getReason());

        // 头一张收口之后，同归口号才许再立一张
        ItemFlowView v = service.view(bills.values().iterator().next().getId(), new Date());
        service.close(Long.valueOf(v.getBillId()), ItemFlowRuler.CLOSE_TERMINATED, "主动撤回", new Date());
        ItemFlowView again = service.open(fullForm("D-ONE"));
        assertNotNull(again.getBillId());
    }

    @Test
    public void 核验四样不齐推不动补齐才走() {
        ItemFlowForm f = fullForm("D-GATE");
        f.setProtectUnit(null);
        ItemFlowView v = service.open(f);
        long id = Long.parseLong(v.getBillId());
        assertEquals(0, v.getCurrentStage());

        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.pushForward(id, 1, new Date()));
        assertEquals(ItemFlowException.Reason.GATE_NOT_MET, ex.getReason());

        ItemFlowForm unit = new ItemFlowForm();
        unit.setProtectUnit("河湾区文化站");
        ItemFlowView after = service.report(id, unit);
        assertTrue(after.isGatePassed());
        ItemFlowView moved = service.pushForward(id, 1, new Date());
        assertEquals(1, moved.getCurrentStage());
    }

    @Test
    public void 评议只认专家意见收齐() {
        Date t0 = new Date(90_000_000_000L);
        ItemFlowView v = service.open(fullForm("D-EXP"));
        long id = Long.parseLong(v.getBillId());
        service.pushForward(id, 1, t0);
        assertEquals(1, service.view(id, t0).getCurrentStage());

        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.pushForward(id, 2, t0));
        assertEquals(ItemFlowException.Reason.GATE_NOT_MET, ex.getReason());
        service.report(id, expertForm(1));
        assertEquals(2, service.pushForward(id, 2, t0).getCurrentStage());
    }

    @Test
    public void 前进一次只收一格越格直推按意向相左挡回() {
        Date t0 = new Date(91_000_000_000L);
        ItemFlowView v = service.open(fullForm("D-SKIP"));
        long id = Long.parseLong(v.getBillId());
        ItemFlowException ex = assertThrows(ItemFlowException.class,
                () -> service.pushForward(id, 3, t0));
        assertEquals(ItemFlowException.Reason.STAGE_SKIPPED, ex.getReason());
        // 一笔没动
        assertEquals(0, service.view(id, t0).getCurrentStage());
    }

    @Test
    public void 公示日子没走完材料再齐也不算踩点那日才算走完() {
        Date t0 = new Date(92_000_000_000L);
        TIchItemFlow listed = runToListed("D-PUB", t0);
        assertEquals(ItemFlowRuler.CLOSE_LISTED, listed.getCloseOutcome());
        assertEquals(2, listed.getStatus());
        assertEquals(3, listed.getStage());
        assertNotNull(listed.getEntryCode());
        // 一格一记共四笔，全是已过口
        assertEquals(4, records.stream().filter(r -> r.getBillId() == listed.getId()).count());
    }

    @Test
    public void 同格第二次上报不另起一行仍留头一遍那句() {
        ItemFlowView v = service.open(fullForm("D-ONCE"));
        long id = Long.parseLong(v.getBillId());
        ItemFlowForm a = new ItemFlowForm();
        a.setRecordText("头一遍那句");
        service.report(id, a);
        ItemFlowForm b = new ItemFlowForm();
        b.setRecordText("第二遍另写的");
        service.report(id, b);

        assertEquals(1, recordCountOfStage(id, 0));
        ItemFlowView now = service.view(id, new Date());
        assertEquals("头一遍那句", now.getContent());
        assertEquals(1, now.getRecords().size());
        assertEquals("头一遍那句", now.getRecords().get(0).getRecordText());
    }

    @Test
    public void 后退只退一格旧记压到下面重走从头攒轮次加一() {
        Date t0 = new Date(93_000_000_000L);
        ItemFlowView v = service.open(fullForm("D-BACK"));
        long id = Long.parseLong(v.getBillId());
        service.pushForward(id, 1, t0);
        assertEquals(1, service.view(id, t0).getCurrentStage());

        ItemFlowView back = service.pullBack(id, t0);
        assertEquals(0, back.getCurrentStage());
        assertEquals(0, back.getStatus());
        // 头一版核验那笔压到下面，一笔没删
        TIchItemFlowRecord old = records.stream()
                .filter(r -> r.getBillId() == id && r.getStage() == 0).findFirst().orElseThrow(AssertionError::new);
        assertEquals(1, old.getPressed());

        // 头一格再无紧挨的上一格可退
        assertEquals(ItemFlowException.Reason.NO_PREV_STAGE,
                assertThrows(ItemFlowException.class, () -> service.pullBack(id, t0)).getReason());

        // 重走从核验从头攒：另起一笔，轮次 0→1
        service.report(id, new ItemFlowForm());
        TIchItemFlowRecord second = records.stream()
                .filter(r -> r.getBillId() == id && r.getStage() == 0
                        && (r.getPressed() == null || r.getPressed() == 0))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(1, second.getRoundNo());
        assertEquals(2, recordCountOfStage(id, 0));
    }

    @Test
    public void 注销终止收口当场锁档卷面改不动整张删不掉也推退不动() {
        Date t0 = new Date(94_000_000_000L);
        ItemFlowView v = service.open(fullForm("D-LOCK"));
        long id = Long.parseLong(v.getBillId());
        service.pushForward(id, 1, t0);
        ItemFlowView closed = service.close(id, ItemFlowRuler.CLOSE_CANCELLED, "材料不实", t0);
        assertTrue(closed.isClosed());
        assertEquals("注销", closed.getCloseOutcomeName());

        assertEquals(ItemFlowException.Reason.LOCKED,
                assertThrows(ItemFlowException.class, () -> service.pushForward(id, 2, t0)).getReason());
        assertEquals(ItemFlowException.Reason.LOCKED,
                assertThrows(ItemFlowException.class, () -> service.pullBack(id, t0)).getReason());
        assertFalse(service.updateContent(id, "想改卷面"));
        assertFalse(service.remove(id));
    }

    @Test
    public void 收口只认注销终止两说列入不许嘴上点() {
        ItemFlowView v = service.open(fullForm("D-SAY"));
        long id = Long.parseLong(v.getBillId());
        assertEquals(ItemFlowException.Reason.BAD_CLOSE_OUTCOME,
                assertThrows(ItemFlowException.class,
                        () -> service.close(id, ItemFlowRuler.CLOSE_LISTED, null, new Date())).getReason());
        assertEquals(ItemFlowException.Reason.BAD_CLOSE_OUTCOME,
                assertThrows(ItemFlowException.class,
                        () -> service.close(id, 9, null, new Date())).getReason());
    }

    @Test
    public void 未收口时卷面改得也删得() {
        ItemFlowView v = service.open(fullForm("D-EDIT"));
        long id = Long.parseLong(v.getBillId());
        assertTrue(service.updateContent(id, "改一句"));
        assertEquals("改一句", bills.get(id).getContent());
        assertTrue(service.remove(id));
        assertNull(bills.get(id));
    }

    @Test
    public void 列入即钉校验码落底册存头一版事后码覆写不了() {
        Date t0 = new Date(95_000_000_000L);
        TIchItemFlow listed = runToListed("D-CODE", t0);
        String code = listed.getEntryCode();
        assertNotNull(code);
        assertTrue(code.startsWith("JM"));
        assertEquals(1, listed.getCurrentVersion());

        // 底册落了一条在册项目，头一版现行
        assertEquals(1, projects.size());
        assertEquals(0, projects.get(0).getStatus());
        List<ItemVersionView> vers = service.versions(listed.getId());
        assertEquals(1, vers.size());
        assertTrue(vers.get(0).isCurrent());
        assertEquals(code, vers.get(0).getEntryCode());

        // 收口锁档：再点推进/上报都不动，码原样
        assertThrows(ItemFlowException.class,
                () -> service.pushForward(listed.getId(), null, new Date(t0.getTime() + 10 * DAY)));
        assertEquals(code, bills.get(listed.getId()).getEntryCode());
    }

    @Test
    public void 变更另起一版旧版挪往期名录只露最新版新旧不重码() {
        Date t0 = new Date(96_000_000_000L);
        TIchItemFlow listed = runToListed("D-REV", t0);
        String oldCode = listed.getEntryCode();

        // 名称、保护单位都没变，不另起一版
        ItemFlowForm same = new ItemFlowForm();
        same.setItemName(listed.getItemName());
        same.setProtectUnit(listed.getProtectUnit());
        assertEquals(ItemFlowException.Reason.NOTHING_CHANGED,
                assertThrows(ItemFlowException.class,
                        () -> service.revise(listed.getId(), same, new Date())).getReason());

        ItemFlowForm change = new ItemFlowForm();
        change.setProtectUnit("澜川县非遗保护中心");
        Date t1 = new Date(t0.getTime() + 30 * DAY);
        ItemVersionView nv = service.revise(listed.getId(), change, t1);
        assertEquals(2, nv.getVersionNo());
        assertTrue(nv.isCurrent());
        assertFalse(oldCode.equals(nv.getEntryCode()));

        List<ItemVersionView> all = service.versions(listed.getId());
        assertEquals(2, all.size());
        assertFalse(all.get(0).isCurrent());
        assertTrue(all.get(1).isCurrent());
        assertEquals(2, bills.get(listed.getId()).getCurrentVersion());
        assertEquals(nv.getEntryCode(), bills.get(listed.getId()).getEntryCode());
        // 底册仍只此一条，名目仍是原名（这一版只换了保护单位，名称没动）
        assertEquals(1, projects.size());
        assertEquals("澜川苗绣", projects.get(0).getSiteName());
        assertEquals("澜川县非遗保护中心", bills.get(listed.getId()).getProtectUnit());
    }

    @Test
    public void 未列入的单不许变更() {
        ItemFlowView v = service.open(fullForm("D-NOREV"));
        long id = Long.parseLong(v.getBillId());
        ItemFlowForm change = new ItemFlowForm();
        change.setProtectUnit("别处");
        assertEquals(ItemFlowException.Reason.NOT_LISTED,
                assertThrows(ItemFlowException.class,
                        () -> service.revise(id, change, new Date())).getReason());
    }

    @Test
    public void 在册数随列入加一随注销减一两处同一回对算() {
        Date t0 = new Date(97_000_000_000L);
        // 一张列入
        runToListed("D-COUNT1", t0);
        // 一张走到评议后注销：不进在册
        ItemFlowView v2 = service.open(fullForm("D-COUNT2"));
        long id2 = Long.parseLong(v2.getBillId());
        service.pushForward(id2, 1, t0);
        service.close(id2, ItemFlowRuler.CLOSE_TERMINATED, "申请人放弃", t0);

        RosterView r = service.roster();
        assertEquals(1, r.getByRoster());
        assertEquals(1, r.getByLedger());
        assertTrue(r.isConsistent());
    }

    @Test
    public void 列入之后再注销随注销减一列入档案与校验码不抹() {
        Date t0 = new Date(97_500_000_000L);
        TIchItemFlow listed = runToListed("D-DELIST", t0);
        String code = listed.getEntryCode();
        assertEquals(1, service.roster().getByRoster());

        Date t1 = new Date(t0.getTime() + 40 * DAY);
        ItemFlowView cancelled = service.close(listed.getId(),
                ItemFlowRuler.CLOSE_CANCELLED, "传承人离世，项目无人接续", t1);
        assertTrue(cancelled.isClosed());
        assertEquals("注销", cancelled.getCloseOutcomeName());
        assertFalse(cancelled.isListed());

        TIchItemFlow now = bills.get(listed.getId());
        // 列入事实与钉死的码原样留底，格次仍停在列入
        assertEquals(3, now.getStage());
        assertEquals(code, now.getEntryCode());
        assertEquals("传承人离世，项目无人接续", now.getCancelReason());
        assertEquals(t1, now.getCancelTime());
        // 在册数随注销减一，两处仍对得上；底册销号
        RosterView r = service.roster();
        assertEquals(0, r.getByRoster());
        assertEquals(0, r.getByLedger());
        assertTrue(r.isConsistent());
        assertEquals(1, projects.get(0).getStatus());
        // 旧版仍翻得到，只是不再现行
        List<ItemVersionView> vers = service.versions(listed.getId());
        assertEquals(1, vers.size());
        assertFalse(vers.get(0).isCurrent());
        assertEquals(code, vers.get(0).getEntryCode());

        // 注销之后锁死：不能再注销、不能终止、不能变更
        assertEquals(ItemFlowException.Reason.LOCKED,
                assertThrows(ItemFlowException.class,
                        () -> service.close(listed.getId(), ItemFlowRuler.CLOSE_TERMINATED, null, t1)).getReason());
        ItemFlowForm change = new ItemFlowForm();
        change.setProtectUnit("别处");
        assertEquals(ItemFlowException.Reason.NOT_LISTED,
                assertThrows(ItemFlowException.class,
                        () -> service.revise(listed.getId(), change, t1)).getReason());
    }

    @Test
    public void 旧签名advance与rollback仍回同一个口子() {
        Date t0 = new Date(98_000_000_000L);
        ItemFlowView v = service.open(fullForm("D-LEGACY"));
        long id = Long.parseLong(v.getBillId());
        TIchItemFlow advanced = service.advance(id, "旧话推进");
        // 核验四样立单时已齐，旧签名照样只推到挨着的评议格
        assertEquals(1, advanced.getStage());
        TIchItemFlow backed = service.rollback(id, "旧话退回");
        assertEquals(0, backed.getStage());
    }

    @Test
    public void 尺上门类各按各的排法且同料同码改版必换码() {
        assertEquals(0, ItemFlowRuler.categoryOrder("民间文学"));
        assertEquals(1, ItemFlowRuler.categoryOrder("传统技艺"));
        assertEquals(2, ItemFlowRuler.categoryOrder("传统医药"));
        assertEquals(3, ItemFlowRuler.categoryOrder("传统音乐"));
        assertEquals(-1, ItemFlowRuler.categoryOrder("别的"));
        // 同料同回必同码，版次不同必不同码
        Date t = new Date(99_000_000_000L);
        String c1 = ItemFlowRuler.entryCode("SB1", "D", 1, "名", "传统音乐", "地", "单位", t);
        assertEquals(c1, ItemFlowRuler.entryCode("SB1", "D", 1, "名", "传统音乐", "地", "单位", t));
        String c2 = ItemFlowRuler.entryCode("SB1", "D", 2, "名", "传统音乐", "地", "单位", t);
        assertFalse(c1.equals(c2));
    }

    // ---------------------------------------------------------------- 通用件

    /**
     * 极简地读 QueryWrapper/UpdateWrapper 里拼出的 WHERE 片段（column op value），
     * 只覆盖本服务用到的 eq/ne/order——够把内存账本按同样条件筛一遍即可。
     */
    private <T> List<T> filter(List<T> all, Wrapper<T> wrapper) {
        List<T> out = new ArrayList<>();
        if (wrapper == null) {
            return all;
        }
        String sql = wrapper.getSqlSegment();
        final java.util.Map<String, Object> pairs = (wrapper instanceof com.baomidou.mybatisplus.core.conditions.AbstractWrapper)
                ? ((com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs()
                : null;
        // 形如 column = #{ew.paramNameValuePairs.MPGENVALn} / column <> #{...} 的简单等值片段
        Pattern p = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*(=|<>)\\s*#\\{[^}]*?\\.paramNameValuePairs\\.([A-Za-z0-9_]+)\\s*\\}");
        Matcher m = p.matcher(sql == null ? "" : sql);
        List<String[]> conds = new ArrayList<>();
        while (m.find()) {
            // [列, 算符, 占位参数名]
            conds.add(new String[]{m.group(1), m.group(2), m.group(3)});
        }
        for (T row : all) {
            boolean ok = true;
            for (String[] cond : conds) {
                String col = cond[0];
                String op = cond[1];
                Object want = pairs == null ? null : pairs.get(cond[2]);
                Object got = readColumn(row, col);
                boolean equal = (got == null && want == null)
                        || (got != null && want != null && String.valueOf(got).equals(String.valueOf(want)));
                if ("=".equals(op) ? !equal : equal) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                out.add(row);
            }
        }
        return out;
    }

    /** 按下划线列名读实体字段（item_name→itemName 等） */
    private static Object readColumn(Object bean, String column) {
        String field = columnToField(column);
        Class<?> c = bean.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                return f.get(bean);
            } catch (NoSuchFieldException ignore) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    private static String columnToField(String column) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char ch : column.toCharArray()) {
            if (ch == '_') {
                up = true;
            } else {
                sb.append(up ? Character.toUpperCase(ch) : ch);
                up = false;
            }
        }
        return sb.toString();
    }

    /** 把补丁对象里非空的字段拷到目标对象（模拟 MyBatis-Plus 的非空字段更新） */
    private static void copyNonNull(Object patch, Object target) {
        Class<?> c = patch.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object val = f.get(patch);
                    if (val != null) {
                        f.set(target, val);
                    }
                } catch (IllegalAccessException ignore) {
                    // 跳过取不到的字段
                }
            }
            c = c.getSuperclass();
        }
    }
}

