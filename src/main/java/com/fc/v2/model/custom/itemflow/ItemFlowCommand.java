package com.fc.v2.model.custom.itemflow;

/**
 * 开单 / 变更入参：形式核验那四样 + 公示几日。门类各按各的排法，
 * 只收民间文学、传统技艺、传统医药、传统音乐四样。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ItemFlowCommand {

    /** 申报单号（本版）；空着则按申报归口号配 */
    private String bizNo;

    /** 申报归口号（同一份申报各版共用） */
    private String declareNo;

    /** 项目名称 */
    private String itemName;

    /** 门类 */
    private String siteType;

    /** 申报地 */
    private String applyArea;

    /** 保护单位 */
    private String protectUnit;

    /** 当地定的公示几日 */
    private Integer publicDays;

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getDeclareNo() {
        return declareNo;
    }

    public void setDeclareNo(String declareNo) {
        this.declareNo = declareNo;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public String getApplyArea() {
        return applyArea;
    }

    public void setApplyArea(String applyArea) {
        this.applyArea = applyArea;
    }

    public String getProtectUnit() {
        return protectUnit;
    }

    public void setProtectUnit(String protectUnit) {
        this.protectUnit = protectUnit;
    }

    public Integer getPublicDays() {
        return publicDays;
    }

    public void setPublicDays(Integer publicDays) {
        this.publicDays = publicDays;
    }
}
