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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 申报准入两头判定明细对象 t_ich_pre_declare_head
 *
 * <p>一头一行：本人从艺年数走一条线，带徒人数另有一条线管着。线代号与启用之日
 * 在判定那一刻钉死在这一行，线册后来换了线也不回头改这一段；旧的那一头提示
 * 不许在页面上不走，故明细只追加不改写。
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_ich_pre_declare_head")
@ApiModel(value = "TIchPreDeclareHead", description = "申报准入两头判定明细")
public class TIchPreDeclareHead implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属判定回执 t_ich_pre_declare.id */
    @TableField("declare_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属判定回执")
    private Long declareId;

    /** 哪一头 0本人从艺年数 1带徒人数 */
    @TableField("head_no")
    @ApiModelProperty(value = "哪一头 0本人从艺年数 1带徒人数")
    private Integer headNo;

    /** 头名（从艺年数/带徒人数） */
    @TableField("head_name")
    @ApiModelProperty(value = "头名")
    private String headName;

    /** 这一头申报的实际数（年数/人数） */
    @TableField("metric")
    @ApiModelProperty(value = "这一头申报的实际数")
    private BigDecimal metric;

    /** 判定那一刻钉下的线代号（无可用线时空着） */
    @TableField("rule_code")
    @ApiModelProperty(value = "判定那一刻钉下的线代号")
    private String ruleCode;

    /** 判定那一刻钉下的启用之日（无可用线时空着） */
    @TableField("eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "判定那一刻钉下的启用之日")
    private Date effStart;

    /** 落到哪一层 1起分层 2过线层 3优线层 4越层；无可用线为空 */
    @TableField("tier")
    @ApiModelProperty(value = "落到哪一层 1起分层 2过线层 3优线层 4越层")
    private Integer tier;

    /** 这一头在线段内否 0越线 1过；无可用线为空 */
    @TableField("passed")
    @ApiModelProperty(value = "这一头在线段内否 0越线 1过")
    private Integer passed;

    /** 没过的提示：把越线那一头的分界数与实际数亮出来，不许只回一句没通过 */
    @TableField("prompt")
    @ApiModelProperty(value = "没过的提示")
    private String prompt;

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

    public Long getDeclareId() {
        return declareId;
    }

    public void setDeclareId(Long declareId) {
        this.declareId = declareId;
    }

    public Integer getHeadNo() {
        return headNo;
    }

    public void setHeadNo(Integer headNo) {
        this.headNo = headNo;
    }

    public String getHeadName() {
        return headName;
    }

    public void setHeadName(String headName) {
        this.headName = headName;
    }

    public BigDecimal getMetric() {
        return metric;
    }

    public void setMetric(BigDecimal metric) {
        this.metric = metric;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }

    public Integer getTier() {
        return tier;
    }

    public void setTier(Integer tier) {
        this.tier = tier;
    }

    public Integer getPassed() {
        return passed;
    }

    public void setPassed(Integer passed) {
        this.passed = passed;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
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
