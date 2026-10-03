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
 * 名录项目申报单逐格留痕对象 t_ich_item_flow_record
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_ich_item_flow_record")
@ApiModel(value = "TIchItemFlowRecord", description = "名录项目申报单逐格留痕")
public class TIchItemFlowRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属申报单 t_ich_item_flow.id */
    @TableField("bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属申报单")
    private Long billId;

    /** 落在哪一格 0核验 1评议 2公示 3列入 */
    @TableField("stage")
    @ApiModelProperty(value = "落在哪一格 0核验 1评议 2公示 3列入")
    private Integer stage;

    /** 重走轮次 0头一回起 每退回该格再走加一 */
    @TableField("round_no")
    @ApiModelProperty(value = "重走轮次 0头一回起 每退回该格再走加一")
    private Integer roundNo;

    /** 头一遍上报那句（第二遍不另起一行） */
    @TableField("record_text")
    @ApiModelProperty(value = "头一遍上报那句（第二遍不另起一行）")
    private String recordText;

    /** 0卷面在报 1已过口（点已过之格只点已过口的现行笔） */
    @TableField("pass_flag")
    @ApiModelProperty(value = "0卷面在报 1已过口")
    private Integer passFlag;

    /** 是否被后退压到下面 0现行 1已压下 */
    @TableField("pressed")
    @ApiModelProperty(value = "是否被后退压到下面 0现行 1已压下")
    private Integer pressed;

    /** 本格门槛材料齐否的逐条钉档（JSON） */
    @TableField("materials")
    @ApiModelProperty(value = "本格门槛材料齐否的逐条钉档（JSON）")
    private String materials;

    /** 这一笔落格时刻 */
    @TableField("enter_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "这一笔落格时刻")
    private Date enterTime;

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

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getRecordText() {
        return recordText;
    }

    public void setRecordText(String recordText) {
        this.recordText = recordText;
    }

    public Integer getPassFlag() {
        return passFlag;
    }

    public void setPassFlag(Integer passFlag) {
        this.passFlag = passFlag;
    }

    public Integer getPressed() {
        return pressed;
    }

    public void setPressed(Integer pressed) {
        this.pressed = pressed;
    }

    public String getMaterials() {
        return materials;
    }

    public void setMaterials(String materials) {
        this.materials = materials;
    }

    public Date getEnterTime() {
        return enterTime;
    }

    public void setEnterTime(Date enterTime) {
        this.enterTime = enterTime;
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
