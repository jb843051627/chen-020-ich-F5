package com.fc.v2.model.custom.itemflow;

import java.util.Date;
import java.util.List;

/**
 * 一张名录申报单此刻的权威全貌：身处第几格由留痕回算，后一格收不收看本格门槛。
 * 纸上的格号、接口递上来的格号都不进这里——只摆推进方法回算出的那一个。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ItemFlowView {

    /** 单据id（字符串下发，保长整精度） */
    private String billId;

    /** 申报单号 */
    private String bizNo;

    /** 同一份申报的归口号 */
    private String declareNo;

    /** 回算出的当前格 0..3 */
    private int currentStage;

    /** 当前格名 */
    private String currentStageName;

    /** 申领会落 0未起 1在办 2已收口 */
    private int status;

    /** 是否已收口（列入/注销/终止），收口即锁档 */
    private boolean closed;

    /** 收口说法 1列入 2注销 3终止；未收口空着 */
    private Integer closeOutcome;

    /** 收口说法名目 */
    private String closeOutcomeName;

    /** 是否已列入现行名录 */
    private boolean listed;

    /** 当下版次 */
    private int versionNo;

    /** 现行名录上露不露这版 */
    private boolean listedFlag;

    /** 列入那一刻钉死的校验码（未列入空着） */
    private String entryCode;

    /** 所落名录底册项目id */
    private String projectId;

    // 四样与各格门槛材料（原样摆出，页面只能看不能据此挪格）
    private String itemName;
    private String category;
    private String applyArea;
    private String protectUnit;
    private Integer publicDays;
    private Date publicStart;
    private boolean expertOk;
    private boolean meetingOk;

    /** 本格门槛此刻过没过（按查看那一刻掐公示日子） */
    private boolean gatePassed;

    /** 本格门槛没过差哪几样 */
    private List<String> gateMissing;

    /** 逐格留痕（含后退压到下面的旧笔） */
    private List<StageRecordView> records;

    /** 卷面那句（当前格头一遍上报那句） */
    private String content;

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

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

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    public String getCurrentStageName() {
        return currentStageName;
    }

    public void setCurrentStageName(String currentStageName) {
        this.currentStageName = currentStageName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public Integer getCloseOutcome() {
        return closeOutcome;
    }

    public void setCloseOutcome(Integer closeOutcome) {
        this.closeOutcome = closeOutcome;
    }

    public String getCloseOutcomeName() {
        return closeOutcomeName;
    }

    public void setCloseOutcomeName(String closeOutcomeName) {
        this.closeOutcomeName = closeOutcomeName;
    }

    public boolean isListed() {
        return listed;
    }

    public void setListed(boolean listed) {
        this.listed = listed;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public boolean isListedFlag() {
        return listedFlag;
    }

    public void setListedFlag(boolean listedFlag) {
        this.listedFlag = listedFlag;
    }

    public String getEntryCode() {
        return entryCode;
    }

    public void setEntryCode(String entryCode) {
        this.entryCode = entryCode;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
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

    public Integer getPublicDays() {
        return publicDays;
    }

    public void setPublicDays(Integer publicDays) {
        this.publicDays = publicDays;
    }

    public Date getPublicStart() {
        return publicStart;
    }

    public void setPublicStart(Date publicStart) {
        this.publicStart = publicStart;
    }

    public boolean isExpertOk() {
        return expertOk;
    }

    public void setExpertOk(boolean expertOk) {
        this.expertOk = expertOk;
    }

    public boolean isMeetingOk() {
        return meetingOk;
    }

    public void setMeetingOk(boolean meetingOk) {
        this.meetingOk = meetingOk;
    }

    public boolean isGatePassed() {
        return gatePassed;
    }

    public void setGatePassed(boolean gatePassed) {
        this.gatePassed = gatePassed;
    }

    public List<String> getGateMissing() {
        return gateMissing;
    }

    public void setGateMissing(List<String> gateMissing) {
        this.gateMissing = gateMissing;
    }

    public List<StageRecordView> getRecords() {
        return records;
    }

    public void setRecords(List<StageRecordView> records) {
        this.records = records;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
