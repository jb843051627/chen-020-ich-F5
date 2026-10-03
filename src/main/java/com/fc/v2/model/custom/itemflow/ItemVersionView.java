package com.fc.v2.model.custom.itemflow;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 一张申报单的一版：旧版从现行名录挪开、转到往期这一层给人翻，名录上只露最新那一版。
 * 新旧各按各的校验码走，两版同一个码便是错的。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ItemVersionView {

    /** 版次id */
    private String versionId;

    /** 所属单据id */
    private String billId;

    /** 版次 1头一版起 */
    private int versionNo;

    /** 这一版的项目名称 */
    private String itemName;

    /** 这一版的门类 */
    private String category;

    /** 这一版的申报地 */
    private String applyArea;

    /** 这一版的保护单位 */
    private String protectUnit;

    /** 这一版钉死的校验码 */
    private String entryCode;

    /** 是不是现行名录上露的那一版 */
    private boolean current;

    /** 落版时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date listedTime;

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getEntryCode() {
        return entryCode;
    }

    public void setEntryCode(String entryCode) {
        this.entryCode = entryCode;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public Date getListedTime() {
        return listedTime;
    }

    public void setListedTime(Date listedTime) {
        this.listedTime = listedTime;
    }
}
