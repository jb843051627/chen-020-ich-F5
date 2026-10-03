package com.fc.v2.model.custom.itemflow;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 一格的一笔留痕：哪一格、重走到第几轮、头一遍那句、有没有被后退压到下面。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class StageRecordView {

    /** 落在哪一格 0..3 */
    private int stage;

    /** 格名 */
    private String stageName;

    /** 重走轮次 0头一回起 */
    private int roundNo;

    /** 头一遍上报那句 */
    private String recordText;

    /** 有没有被后退压到下面 */
    private boolean pressed;

    /** 落格时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date enterTime;

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public int getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(int roundNo) {
        this.roundNo = roundNo;
    }

    public String getRecordText() {
        return recordText;
    }

    public void setRecordText(String recordText) {
        this.recordText = recordText;
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public Date getEnterTime() {
        return enterTime;
    }

    public void setEnterTime(Date enterTime) {
        this.enterTime = enterTime;
    }
}
