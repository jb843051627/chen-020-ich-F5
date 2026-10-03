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
 * <p>stage/status/content 各栏只作页面参考：权威格次顺着 t_ich_item_flow_record
 * 已过之格点出，前端与纸上的格号都只算意向。挪格次只有服务层那一个推进口。
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

    /** 名录项目申报单号（本版） */
    @TableField("biz_no")
    @ApiModelProperty(value = "名录项目申报单号（本版）")
    private String bizNo;

    /** 申报归口号（同一份申报各版共用） */
    @TableField("declare_no")
    @ApiModelProperty(value = "申报归口号")
    private String declareNo;

    /** 版次 0头一版 每变更另起一版加一 */
    @TableField("version_no")
    @ApiModelProperty(value = "版次")
    private Integer versionNo;

    /** 现行否 0往期 1现行（现行名录只露1） */
    @TableField("current_flag")
    @ApiModelProperty(value = "现行否 0往期 1现行")
    private Integer currentFlag;

    /** 重走轮次 0头一回 每后退一格加一 */
    @TableField("round_no")
    @ApiModelProperty(value = "重走轮次")
    private Integer roundNo;

    /** 当前格次（仅页面参考）0形式核验 1专家评议 2社会公示 3列入名录 */
    @TableField("stage")
    @ApiModelProperty(value = "当前格次（仅页面参考）")
    private Integer stage;

    /** 申领会落 0未起 1在办 2已收口 */
    @TableField("status")
    @ApiModelProperty(value = "申领会落 0未起 1在办 2已收口")
    private Integer status;

    /** 形式要件·项目名称 */
    @TableField("item_name")
    @ApiModelProperty(value = "形式要件·项目名称")
    private String itemName;

    /** 形式要件·门类（民间文学/传统技艺/传统医药/传统音乐） */
    @TableField("site_type")
    @ApiModelProperty(value = "形式要件·门类")
    private String siteType;

    /** 形式要件·申报地 */
    @TableField("apply_area")
    @ApiModelProperty(value = "形式要件·申报地")
    private String applyArea;

    /** 形式要件·保护单位 */
    @TableField("protect_unit")
    @ApiModelProperty(value = "形式要件·保护单位")
    private String protectUnit;

    /** 专家意见收齐否 0未齐 1已齐 */
    @TableField("experts_received")
    @ApiModelProperty(value = "专家意见收齐否 0未齐 1已齐")
    private Integer expertsReceived;

    /** 当地定的公示几日 */
    @TableField("public_days")
    @ApiModelProperty(value = "当地定的公示几日")
    private Integer publicDays;

    /** 公示起算时刻（进社会公示格落笔；后退重走则清空重算） */
    @TableField("public_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "公示起算时刻")
    private Date publicStart;

    /** 名录会议认不认 0不认 1认 */
    @TableField("meeting_recognized")
    @ApiModelProperty(value = "名录会议认不认 0不认 1认")
    private Integer meetingRecognized;

    /** 收口说法 0未收口 1列入 2注销 3终止 */
    @TableField("close_type")
    @ApiModelProperty(value = "收口说法 0未收口 1列入 2注销 3终止")
    private Integer closeType;

    /** 校验码：列入一刻算定，此后只可读不可覆写；新版另算 */
    @TableField("check_code")
    @ApiModelProperty(value = "校验码")
    private String checkCode;

    /** 列入时刻（与校验码一同钉下） */
    @TableField("listed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "列入时刻")
    private Date listedTime;

    /** 列入后所拴名录项目代号 t_ich_project.site_no */
    @TableField("site_no")
    @ApiModelProperty(value = "列入后所拴名录项目代号")
    private String siteNo;

    /** 上一版申报单id（头版为空） */
    @TableField("prev_version_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "上一版申报单id")
    private Long prevVersionId;

    /** 卷面参考字（权威卷面以留痕头一遍那句为准） */
    @TableField("content")
    @ApiModelProperty(value = "卷面参考字")
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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Integer getCurrentFlag() {
        return currentFlag;
    }

    public void setCurrentFlag(Integer currentFlag) {
        this.currentFlag = currentFlag;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
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

    public Integer getExpertsReceived() {
        return expertsReceived;
    }

    public void setExpertsReceived(Integer expertsReceived) {
        this.expertsReceived = expertsReceived;
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

    public Integer getMeetingRecognized() {
        return meetingRecognized;
    }

    public void setMeetingRecognized(Integer meetingRecognized) {
        this.meetingRecognized = meetingRecognized;
    }

    public Integer getCloseType() {
        return closeType;
    }

    public void setCloseType(Integer closeType) {
        this.closeType = closeType;
    }

    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }

    public Date getListedTime() {
        return listedTime;
    }

    public void setListedTime(Date listedTime) {
        this.listedTime = listedTime;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public Long getPrevVersionId() {
        return prevVersionId;
    }

    public void setPrevVersionId(Long prevVersionId) {
        this.prevVersionId = prevVersionId;
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
