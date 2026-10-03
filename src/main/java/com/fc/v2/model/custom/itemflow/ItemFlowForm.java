package com.fc.v2.model.custom.itemflow;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 名录申报单立单 / 本格上报递来的材料。
 *
 * <p>四样（项目名称、门类、申报地、保护单位）在形式核验那一格点；
 * 专家意见、名录会议、公示几日与起笔时刻各在各的格子点。
 * 这里面没有格号可填——身处第几格由推进方法点已过之格得出，不许递个格号上来挪格。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ItemFlowForm {

    /** 申报单号（空着按立单主键回写） */
    private String bizNo;

    /** 同一份申报的归口号（一号只容一张在跑的单） */
    private String declareNo;

    /** 项目名称 */
    private String itemName;

    /** 门类（民间文学/传统技艺/传统医药/传统音乐） */
    private String category;

    /** 申报地 */
    private String applyArea;

    /** 保护单位 */
    private String protectUnit;

    /** 当地定的公示几日 */
    private Integer publicDays;

    /** 公示起笔那一刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publicStart;

    /** 专家意见收没收齐 0/1 */
    private Integer expertOk;

    /** 名录会议认不认 0/1 */
    private Integer meetingOk;

    /** 本格头一遍上报那句（第二遍仍留头一遍那句，不另起一行） */
    private String recordText;

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

    public Integer getExpertOk() {
        return expertOk;
    }

    public void setExpertOk(Integer expertOk) {
        this.expertOk = expertOk;
    }

    public Integer getMeetingOk() {
        return meetingOk;
    }

    public void setMeetingOk(Integer meetingOk) {
        this.meetingOk = meetingOk;
    }

    public String getRecordText() {
        return recordText;
    }

    public void setRecordText(String recordText) {
        this.recordText = recordText;
    }
}
