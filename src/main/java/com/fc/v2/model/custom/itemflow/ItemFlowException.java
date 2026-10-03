package com.fc.v2.model.custom.itemflow;

/**
 * 名录申报单在唯一推进口被挡下时的答复。
 *
 * <p>归在第几格、后一格收不收、收口后改不改得动、同格第二遍落不落第二笔、一次收几格、
 * 变更时旧版露不露——一律由服务层那一个推进方法以这里的缘由回话，页面不留第二处挪格次的把手。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ItemFlowException extends RuntimeException {

    public enum Reason {
        /** 查无此单 */
        NOT_FOUND,
        /** 单子已收口（列入/注销/终止）当场锁档，字改不动、退不回、删不掉 */
        LOCKED,
        /** 接口递来的意向格号与入口顺着留痕点出的当前格相左，按入口的来 */
        STAGE_MISMATCH,
        /** 这一格的门槛没过（四要件/专家/公示期/名录会议），后一格开着也不能进 */
        GATE_NOT_MET,
        /** 已在头一格（形式核验），再无紧挨的上一格可退 */
        NO_PREV_STAGE,
        /** 同一份申报已有一张在跑的单，没办完也没喊停，后一张立不住 */
        ALREADY_RUNNING,
        /** 门类不在四样排法之内（民间文学/传统技艺/传统医药/传统音乐） */
        BAD_CATEGORY,
        /** 收口说法只认 2注销 3终止，且不能对已收口单再收口 */
        BAD_CLOSE_TYPE,
        /** 变更只能对着已列入的现行版另起一版 */
        NOT_LISTED,
        /** 落笔瞬间单据已被别人推动，乐观条件没对上 */
        CONCURRENT_CHANGED,
        /** 同一份申报两版算出同一串校验码，即错 */
        DUPLICATE_CHECK_CODE,
        /** 开单/变更入参不合法 */
        BAD_ARGUMENT
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
