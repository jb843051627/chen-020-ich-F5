package com.fc.v2.model.custom.preadmit;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 一头的回话：这一头依哪条线判、启用之日、落到哪一层、在线段内否。
 *
 * <p>当刻一条线也够不着时不给结论：{@link #available} 为 false，线代号与启用之日两栏
 * 空着，不许抓邻近那条的数凑答复；{@link #tier}/{@link #passed} 亦为空。
 * 线代号与启用之日钉在本回话里，落库即以此为准，往后换了线也不回头改这一段。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class HeadJudge {

    /** 哪一头 0本人从艺年数 1带徒人数 */
    private int headNo;

    /** 头名 */
    private String headName;

    /** 这一头申报的实际数 */
    private BigDecimal metric;

    /** 当刻够不够得着线（false 即无结论） */
    private boolean available;

    /** 判定所依线册行主键（来历核对用；无可用线为空） */
    private Long ruleId;

    /** 钉下的线代号（无可用线为空） */
    private String ruleCode;

    /** 钉下的线名（屏上翻看用；无可用线为空） */
    private String ruleName;

    /** 钉下的启用之日（无可用线为空） */
    private Date effStart;

    /** 落到哪一层 1起分层 2过线层 3优线层 4越层；无可用线为空 */
    private Integer tier;

    /** 三条分界数，屏上亮数用（无可用线全空） */
    private BigDecimal th1Max;
    private BigDecimal th2Max;
    private BigDecimal th3Max;

    /** 这一头在线段内否（过线层..优线层为过）；无可用线为空 */
    private Boolean passed;

    /** 没过的提示：把越线那一头的分界数与实际数亮出来；过了或无结论为空 */
    private String prompt;

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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
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

    public BigDecimal getTh1Max() {
        return th1Max;
    }

    public void setTh1Max(BigDecimal th1Max) {
        this.th1Max = th1Max;
    }

    public BigDecimal getTh2Max() {
        return th2Max;
    }

    public void setTh2Max(BigDecimal th2Max) {
        this.th2Max = th2Max;
    }

    public BigDecimal getTh3Max() {
        return th3Max;
    }

    public void setTh3Max(BigDecimal th3Max) {
        this.th3Max = th3Max;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
