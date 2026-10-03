package com.fc.v2.model.custom.preadmit;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 一份申报两头的数：本人从艺年数、带徒人数。
 * 申请注明那一刻随单带来，复算就拿这一刻自己的日子回看线册，不许拿手里最新那条顶上去。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class PreDeclareInput {

    /** 申报人代号 */
    private String applicantCode;

    /** 申请注明那一刻（回看线册的唯一时刻） */
    private Date applyAt;

    /** 本人从艺年数 */
    private BigDecimal years;

    /** 带徒人数 */
    private BigDecimal apprentices;

    public PreDeclareInput() {
    }

    public PreDeclareInput(String applicantCode, Date applyAt, BigDecimal years, BigDecimal apprentices) {
        this.applicantCode = applicantCode;
        this.applyAt = applyAt;
        this.years = years;
        this.apprentices = apprentices;
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

    public BigDecimal getYears() {
        return years;
    }

    public void setYears(BigDecimal years) {
        this.years = years;
    }

    public BigDecimal getApprentices() {
        return apprentices;
    }

    public void setApprentices(BigDecimal apprentices) {
        this.apprentices = apprentices;
    }
}
