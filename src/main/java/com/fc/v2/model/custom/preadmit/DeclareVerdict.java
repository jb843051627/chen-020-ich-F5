package com.fc.v2.model.custom.preadmit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 一份申报的判定回话——四问里的"某位申报人在申请注明那一刻依哪条线判"。
 *
 * <p>判定只有一条路（服务层那一个判据出口），本对象只装该出口的回话：
 * <ul>
 *   <li>{@link #verdict}=0 无结论：当刻两头有一头一条线也够不着，代号与启用之日两栏
 *       在那一头空着，不许抓邻近那条的数凑答复；</li>
 *   <li>{@link #verdict}=1 准入：两头各自都落在过线层..优线层；</li>
 *   <li>{@link #verdict}=2 不准入：有一头越了，越了哪头看 {@link HeadJudge#getPrompt()}
 *       里亮出的分界数与实际数，不许只回一句"没通过"。</li>
 * </ul>
 * 两头逐条判出的明细与结论同一回算，恒为两条。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class DeclareVerdict {

    /** 无结论：当刻一条线也够不着 */
    public static final int VERDICT_NONE = 0;

    /** 准入：两头都在线段内 */
    public static final int VERDICT_PASS = 1;

    /** 不准入：两头有一头越线 */
    public static final int VERDICT_REJECT = 2;

    /** 申报人代号 */
    private String applicantCode;

    /** 回看线册所用的那一刻 */
    private Date applyAt;

    /** 判定情形 */
    private int verdict;

    /** 两头逐条回话，下标 0从艺年数 1带徒人数 */
    private List<HeadJudge> heads = new ArrayList<>();

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

    public int getVerdict() {
        return verdict;
    }

    public void setVerdict(int verdict) {
        this.verdict = verdict;
    }

    public List<HeadJudge> getHeads() {
        return heads;
    }

    public void setHeads(List<HeadJudge> heads) {
        this.heads = heads == null ? new ArrayList<>() : heads;
    }

    /** 按头编号取回话 */
    public HeadJudge head(int headNo) {
        for (HeadJudge h : heads) {
            if (h.getHeadNo() == headNo) {
                return h;
            }
        }
        return null;
    }

    /** 两头逐条都不可改的只读视图 */
    public List<HeadJudge> headsView() {
        return Collections.unmodifiableList(heads);
    }
}
