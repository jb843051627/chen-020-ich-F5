package com.fc.v2.itemflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TIchItemFlow;
import com.fc.v2.model.auto.TIchItemFlowRecord;
import com.fc.v2.model.auto.TIchProject;
import com.fc.v2.model.custom.itemflow.ItemFlowChain;
import com.fc.v2.model.custom.itemflow.ItemFlowLedger;
import com.fc.v2.model.custom.itemflow.ItemFlowView;

/**
 * ItemFlowChain 纯算引擎的规矩测试（不碰库）。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ItemFlowChainTest {

    private TIchItemFlow flow(int stage, Integer status) {
        TIchItemFlow f = new TIchItemFlow();
        f.setId(1L);
        f.setBizNo("SB-1");
        f.setDeclareNo("SB-1");
        f.setVersionNo(0);
        f.setCurrentFlag(1);
        f.setRoundNo(0);
        f.setStage(stage);
        f.setStatus(status);
        f.setItemName("苗绣");
        f.setSiteType("传统技艺");
        f.setApplyArea("澜川县");
        f.setProtectUnit("文化馆");
        f.setDelFlag(0);
        return f;
    }

    private TIchItemFlowRecord rec(int stage, int round, String action, String note) {
        TIchItemFlowRecord r = new TIchItemFlowRecord();
        r.setId((long) (stage * 10 + round + 1));
        r.setFlowId(1L);
        r.setStage(stage);
        r.setRoundNo(round);
        r.setAction(action);
        r.setNote(note);
        r.setActionTime(new Date(1_000_000L + stage * 1000L + round));
        r.setDelFlag(0);
        return r;
    }

    @Test
    public void 形式核验四样缺一不可门类须在四样排法内() {
        TIchItemFlow f = flow(0, 0);
        f.setItemName(null);
        ItemFlowChain.Gate g = ItemFlowChain.gateOf(f, 0, new Date());
        assertFalse(g.isPassed());
        assertTrue(g.getReason().contains("项目名称"));

        f.setItemName("苗绣");
        f.setSiteType("传统舞蹈");
        assertFalse(ItemFlowChain.gateOf(f, 0, new Date()).isPassed());
        assertTrue(ItemFlowChain.gateOf(f, 0, new Date()).getReason().contains("门类"));

        f.setSiteType("传统音乐");
        assertTrue(ItemFlowChain.gateOf(f, 0, new Date()).isPassed());
    }

    @Test
    public void 评议格看专家意见收齐() {
        TIchItemFlow f = flow(1, 1);
        assertFalse(ItemFlowChain.gateOf(f, 1, new Date()).isPassed());
        f.setExpertsReceived(1);
        assertTrue(ItemFlowChain.gateOf(f, 1, new Date()).isPassed());
    }

    @Test
    public void 公示日子没走完后一格开着也不能进() {
        TIchItemFlow f = flow(2, 1);
        Date start = new Date(1_000_000L);
        f.setPublicDays(5);
        f.setPublicStart(start);
        assertFalse(ItemFlowChain.gateOf(f, 2, new Date(start.getTime() + 2L * 86_400_000L)).isPassed());
        // 日子走完才算
        assertTrue(ItemFlowChain.gateOf(f, 2, new Date(start.getTime() + 5L * 86_400_000L)).isPassed());
    }

    @Test
    public void 列入格看名录会议认不认() {
        TIchItemFlow f = flow(3, 1);
        assertFalse(ItemFlowChain.gateOf(f, 3, new Date()).isPassed());
        f.setMeetingRecognized(1);
        assertTrue(ItemFlowChain.gateOf(f, 3, new Date()).isPassed());
    }

    @Test
    public void 当前格顺已过之格点出格子里敲的不算() {
        TIchItemFlow f = flow(3, 1); // 格子里故意敲到列入格
        List<TIchItemFlowRecord> records = Arrays.asList(rec(0, 0, "ADVANCE", "核验过"));
        ItemFlowChain.State st = ItemFlowChain.evaluate(f, records);
        // 只有核验格过了，权威当前格应是评议格(1)，不是格子里的 3
        assertEquals(1, st.getCurrentStage());
        assertTrue(st.isPassed(0));
        assertFalse(st.isPassed(1));
    }

    @Test
    public void 后退一格轮次加一被退格重攒更早格沿用() {
        TIchItemFlow f = flow(1, 1);
        f.setRoundNo(1);
        // 轮0已过核验(0)、评议(1)进到公示又被退回评议：BACK 落 stage1 轮1
        List<TIchItemFlowRecord> records = new ArrayList<>(Arrays.asList(
                rec(0, 0, "ADVANCE", "核验过"),
                rec(1, 0, "ADVANCE", "评议头遍"),
                rec(1, 1, "BACK", "退回评议")));
        ItemFlowChain.State st = ItemFlowChain.evaluate(f, records);
        assertEquals(1, st.getEntryStage());
        assertTrue(st.isPassed(0));   // 核验沿用早先轮次
        assertFalse(st.isPassed(1));  // 评议被压下，重新攒
        assertEquals(1, st.getCurrentStage());
    }

    @Test
    public void 同格头一遍那句为准() {
        TIchItemFlow f = flow(1, 1);
        List<TIchItemFlowRecord> records = Arrays.asList(rec(0, 0, "ADVANCE", "头一句"));
        ItemFlowChain.State st = ItemFlowChain.evaluate(f, records);
        assertEquals("头一句", ItemFlowChain.firstNote(st, 0));
    }

    @Test
    public void 校验码列入算定改一版重算同版同入参才相同() {
        TIchItemFlow v0 = flow(3, 2);
        String c0 = ItemFlowChain.checkCode(v0);

        TIchItemFlow v1 = flow(3, 2);
        v1.setVersionNo(1);
        assertNotEquals(c0, ItemFlowChain.checkCode(v1)); // 版次不同码不同

        TIchItemFlow renamed = flow(3, 2);
        renamed.setProtectUnit("非遗保护中心");
        assertNotEquals(c0, ItemFlowChain.checkCode(renamed)); // 换保护单位码不同

        TIchItemFlow same = flow(3, 2);
        assertEquals(c0, ItemFlowChain.checkCode(same)); // 同版同入参同码
    }

    @Test
    public void 全貌暴露权威格与页面格是否对齐() {
        TIchItemFlow f = flow(2, 1);
        List<TIchItemFlowRecord> records = Arrays.asList(
                rec(0, 0, "ADVANCE", null), rec(1, 0, "ADVANCE", null));
        ItemFlowView v = ItemFlowChain.buildView(f, records);
        assertEquals(2, v.getCurrentStage());
        assertTrue(v.isStageMatched()); // 格子恰好也是2

        f.setStage(3);
        ItemFlowView mismatch = ItemFlowChain.buildView(f, records);
        assertEquals(2, mismatch.getCurrentStage());
        assertFalse(mismatch.isStageMatched()); // 相左听回算
    }

    @Test
    public void 在册数收口单与底册同一回算对得齐() {
        TIchItemFlow listed = flow(3, 2);
        listed.setCloseType(ItemFlowChain.CLOSE_LISTED);
        TIchItemFlow cancelled = flow(3, 2);
        cancelled.setId(2L);
        cancelled.setDeclareNo("SB-2");
        cancelled.setCloseType(ItemFlowChain.CLOSE_CANCEL);

        TIchProject active = new TIchProject();
        active.setStatus(0);
        active.setDelFlag(0);
        // 列入1 注销1 => 收口单口径在册0；底册也摆0在册
        ItemFlowLedger ok = ItemFlowChain.buildLedger(
                Arrays.asList(listed, cancelled), new ArrayList<>());
        assertEquals(1, ok.getListedCount());
        assertEquals(1, ok.getCancelledCount());
        assertEquals(0, ok.getFlowRegisteredCount());
        assertEquals(0, ok.getRegistryRegisteredCount());
        assertTrue(ok.isCountMatched());

        ItemFlowLedger bad = ItemFlowChain.buildLedger(
                Arrays.asList(listed, cancelled), Arrays.asList(active));
        assertFalse(bad.isCountMatched());
    }
}
