package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 名录项目申报单对象 t_ich_item_flow
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_ich_item_flow")
@ApiModel(value = "TIchItemFlow", description = "名录项目申报单")
public class TIchItemFlow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 名录项目申报单号 */
    @TableField("biz_no")
    @ApiModelProperty(value = "名录项目申报单号")
    private String bizNo;

    /** 同一份申报的归口号（一号只容一张在跑的单） */
    @TableField("declare_no")
    @ApiModelProperty(value = "同一份申报的归口号（一号只容一张在跑的单）")
    private String declareNo;

    /** 当前格次参考列 0..3（核验/评议/公示/列入；权威数以留痕回算为准） */
    @TableField("stage")
    @ApiModelProperty(value = "当前格次参考列 0..3（核验/评议/公示/列入；权威数以留痕回算为准）")
    private Integer stage;

    /** 申领会落 0未起 1在办 2已收口 */
    @TableField("status")
    @ApiModelProperty(value = "申领会落 0未起 1在办 2已收口")
    private Integer status;

    /** 收口说法 1列入 2注销 3终止；未收口空着 */
    @TableField("close_outcome")
    @ApiModelProperty(value = "收口说法 1列入 2注销 3终止；未收口空着")
    private Integer closeOutcome;

    /** 项目名称（形式核验四样之一） */
    @TableField("item_name")
    @ApiModelProperty(value = "项目名称（形式核验四样之一）")
    private String itemName;

    /** 门类（民间文学/传统技艺/传统医药/传统音乐） */
    @TableField("category")
    @ApiModelProperty(value = "门类（民间文学/传统技艺/传统医药/传统音乐）")
    private String category;

    /** 申报地（形式核验四样之一） */
    @TableField("apply_area")
    @ApiModelProperty(value = "申报地（形式核验四样之一）")
    private String applyArea;

    /** 保护单位（形式核验四样之一） */
    @TableField("protect_unit")
    @ApiModelProperty(value = "保护单位（形式核验四样之一）")
    private String protectUnit;

    /** 当地定的公示几日（公示格的门槛） */
    @TableField("public_days")
    @ApiModelProperty(value = "当地定的公示几日（公示格的门槛）")
    private Integer publicDays;

    /** 公示起笔那一刻（日子没走完材料再齐也不算） */
    @TableField("public_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "公示起笔那一刻")
    private Date publicStart;

    /** 专家意见收没收齐 0没收齐 1收齐 */
    @TableField("expert_ok")
    @ApiModelProperty(value = "专家意见收没收齐 0没收齐 1收齐")
    private Integer expertOk;

    /** 名录会议认不认 0不认 1认 */
    @TableField("meeting_ok")
    @ApiModelProperty(value = "名录会议认不认 0不认 1认")
    private Integer meetingOk;

    /** 当下版次 1头一版起 变更另起一版 */
    @TableField("current_version")
    @ApiModelProperty(value = "当下版次 1头一版起 变更另起一版")
    private Integer currentVersion;

    /** 现行名录上露不露这版 0不露(旧版/未列入) 1露(最新列入版) */
    @TableField("listed_flag")
    @ApiModelProperty(value = "现行名录上露不露这版 0不露(旧版/未列入) 1露(最新列入版)")
    private Integer listedFlag;

    /** 列入校验码（一旦列入即钉死，任谁都覆写不了；改版重算） */
    @TableField("entry_code")
    @ApiModelProperty(value = "列入校验码（一旦列入即钉死，任谁都覆写不了；改版重算）")
    private String entryCode;

    /** 列入后所落名录底册项目 t_ich_project.id */
    @TableField("project_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "列入后所落名录底册项目")
    private Long projectId;

    /** 列入之后又注销的缘由（卷面留档，年末凭它追为何注销） */
    @TableField("cancel_reason")
    @ApiModelProperty(value = "列入之后又注销的缘由")
    private String cancelReason;

    /** 列入之后注销那一刻（列入事实与校验码不抹） */
    @TableField("cancel_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "列入之后注销那一刻")
    private Date cancelTime;

    /** 一格一记（同格第二遍不另起一行，留头一遍那句） */
    @TableField("content")
    @ApiModelProperty(value = "一格一记（同格第二遍不另起一行，留头一遍那句）")
    private String content;

    /** 最近一次过口动作 */
    @TableField("last_action")
    @ApiModelProperty(value = "最近一次过口动作")
    private String lastAction;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getCloseOutcome() {
        return closeOutcome;
    }

    public void setCloseOutcome(Integer closeOutcome) {
        this.closeOutcome = closeOutcome;
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

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Integer getListedFlag() {
        return listedFlag;
    }

    public void setListedFlag(Integer listedFlag) {
        this.listedFlag = listedFlag;
    }

    public String getEntryCode() {
        return entryCode;
    }

    public void setEntryCode(String entryCode) {
        this.entryCode = entryCode;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Date getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(Date cancelTime) {
        this.cancelTime = cancelTime;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
