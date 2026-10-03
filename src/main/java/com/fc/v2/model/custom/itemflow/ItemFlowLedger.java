package com.fc.v2.model.custom.itemflow;

/**
 * 名录在册数一本账：在册项目数随列入 +1、随注销 −1，一处顺着收口单点，一处当场逐格点底册，
 * 两回须是同一回算，对得齐才算。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ItemFlowLedger {

    /** 顺着收口单点出的列入数 */
    private int listedCount;

    /** 顺着收口单点出的注销数 */
    private int cancelledCount;

    /** 收口单口径在册数 = 列入 − 注销 */
    private int flowRegisteredCount;

    /** 当场逐格点底册在册条目数 */
    private int registryRegisteredCount;

    /** 两处同一回算里对得齐否 */
    private boolean countMatched;

    public int getListedCount() {
        return listedCount;
    }

    public void setListedCount(int listedCount) {
        this.listedCount = listedCount;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(int cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public int getFlowRegisteredCount() {
        return flowRegisteredCount;
    }

    public void setFlowRegisteredCount(int flowRegisteredCount) {
        this.flowRegisteredCount = flowRegisteredCount;
    }

    public int getRegistryRegisteredCount() {
        return registryRegisteredCount;
    }

    public void setRegistryRegisteredCount(int registryRegisteredCount) {
        this.registryRegisteredCount = registryRegisteredCount;
    }

    public boolean isCountMatched() {
        return countMatched;
    }

    public void setCountMatched(boolean countMatched) {
        this.countMatched = countMatched;
    }
}
