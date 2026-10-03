package com.fc.v2.model.custom.preadmit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TIchPreLine;

/**
 * 一批申报跑一轮的战报：在场名单与逐条判定**同源于一次装载点算**，
 * 不许屏上摆一份名单、逐条判定另回线册取一回——两回的数不一样即错。
 *
 * <p>每条申报仍按它自己注明的那一刻回看线册（前年的复算拿前年的日子，
 * 手里最新那条不许顶上去）；在场名单是本轮开跑那一刻在场的线。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class PreAdmitReport {

    /** 本轮开跑那一刻（在场名单以此刻回看） */
    private Date at;

    /** 这一刻在场的线（与逐条判定同一回装载点算，让位顺位降序、代号字数靠后在前） */
    private List<TIchPreLine> presentLines = new ArrayList<>();

    /** 逐条判定回话，一条申报一条 */
    private List<DeclareVerdict> verdicts = new ArrayList<>();

    /** 本轮判定条数 */
    private int judged;

    /** 准入条数 */
    private int admitted;

    /** 不准入条数 */
    private int rejected;

    /** 无结论条数（当刻一条线也够不着） */
    private int noConclusion;

    public Date getAt() {
        return at;
    }

    public void setAt(Date at) {
        this.at = at;
    }

    public List<TIchPreLine> getPresentLines() {
        return presentLines;
    }

    public void setPresentLines(List<TIchPreLine> presentLines) {
        this.presentLines = presentLines == null ? new ArrayList<>() : presentLines;
    }

    public List<DeclareVerdict> getVerdicts() {
        return verdicts;
    }

    public void setVerdicts(List<DeclareVerdict> verdicts) {
        this.verdicts = verdicts == null ? new ArrayList<>() : verdicts;
    }

    public int getJudged() {
        return judged;
    }

    public void setJudged(int judged) {
        this.judged = judged;
    }

    public int getAdmitted() {
        return admitted;
    }

    public void setAdmitted(int admitted) {
        this.admitted = admitted;
    }

    public int getRejected() {
        return rejected;
    }

    public void setRejected(int rejected) {
        this.rejected = rejected;
    }

    public int getNoConclusion() {
        return noConclusion;
    }

    public void setNoConclusion(int noConclusion) {
        this.noConclusion = noConclusion;
    }
}
