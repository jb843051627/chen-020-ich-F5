package com.fc.v2.model.custom.preadmit;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 来历不对的旧案一条：屏上挂着线代号，线册里却查不到申报注明那一天在场的那条。
 *
 * <p>去年那两份照着停用的线判下来的申报，事后要能一眼看出历不对——
 * 逐条列出来，不许跟在现行结论后面混作一笔。对得上与否只看两件事：
 * 代号在不在册、该日在场的启用之日与钉下的那一日合不合。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class StaleHeadRef {

    /** 回执 id（字符串下发，保长整精度） */
    private String declareId;

    /** 批据号 */
    private String declareNo;

    /** 申报人代号 */
    private String applicantCode;

    /** 申请注明那一刻 */
    private Date applyAt;

    /** 哪一头 0从艺年数 1带徒人数 */
    private int headNo;

    /** 头名 */
    private String headName;

    /** 这一头的实际数 */
    private BigDecimal metric;

    /** 屏上挂着的线代号 */
    private String pinnedRuleCode;

    /** 屏上挂着的启用之日 */
    private Date pinnedEffStart;

    /** 来历不对的缘由：代号在册中查无 / 该日不在场（已停用或已交棒） */
    private String reason;

    public String getDeclareId() {
        return declareId;
    }

    public void setDeclareId(String declareId) {
        this.declareId = declareId;
    }

    public String getDeclareNo() {
        return declareNo;
    }

    public void setDeclareNo(String declareNo) {
        this.declareNo = declareNo;
    }

    public String getApplicantCode() {
        return applicantCode;
    }

    public void setApplicantCode(String applicantCode) {
        this.applicantCode = applicantCode;
    }

    public Date getApplyAt() {
        return applyAt;
    }

    public void setApplyAt(Date applyAt) {
        this.applyAt = applyAt;
    }

    public int getHeadNo() {
        return headNo;
    }

    public void setHeadNo(int headNo) {
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

    public String getPinnedRuleCode() {
        return pinnedRuleCode;
    }

    public void setPinnedRuleCode(String pinnedRuleCode) {
        this.pinnedRuleCode = pinnedRuleCode;
    }

    public Date getPinnedEffStart() {
        return pinnedEffStart;
    }

    public void setPinnedEffStart(Date pinnedEffStart) {
        this.pinnedEffStart = pinnedEffStart;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
