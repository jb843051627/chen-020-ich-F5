package com.fc.v2.model.custom.itemflow;

/**
 * 名录申报单过口被推进方法拒绝时的答复。
 *
 * <p>四格门槛、收口锁档、越格、重号、并发各有各的缘由；
 * 一份申报归在第几格、后一格收不收，只由服务层那一个推进方法据这里的回话回话。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ItemFlowException extends RuntimeException {

    public enum Reason {
        /** 查无此单 */
        NOT_FOUND,
        /** 已收口（列入/注销/终止）：卷面上的字改不了、整张删不掉，也推不动退不回 */
        LOCKED,
        /** 本格门槛没过（四样不齐/专家意见没齐/公示日子没走完/名录会议没认） */
        GATE_NOT_MET,
        /** 想越过挨着的格往前推：前进只到挨着的那一格 */
        STAGE_SKIPPED,
        /** 已在头一格（形式核验），再无紧挨的上一格可退 */
        NO_PREV_STAGE,
        /** 同一份申报底下已有一张在跑的单，头一张没办完也没喊停 */
        DUPLICATE_RUNNING,
        /** 门类不在民间文学/传统技艺/传统医药/传统音乐四家之内 */
        BAD_CATEGORY,
        /** 变更只接在已列入的单上，且名称/保护单位总得有一样真变了 */
        NOT_LISTED,
        /** 落笔瞬间单据已被别人推动，乐观条件没对上 */
        CONCURRENT_CHANGED,
        /** 收口说法只认注销/终止两说（列入由列入格认下，不许嘴上点名） */
        BAD_CLOSE_OUTCOME,
        /** 变更总得有一样真变了（名称或保护单位），原样照抄不另起一版 */
        NOTHING_CHANGED
    }

    private static final long serialVersionUID = 1L;

    private final Reason reason;

    public ItemFlowException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
