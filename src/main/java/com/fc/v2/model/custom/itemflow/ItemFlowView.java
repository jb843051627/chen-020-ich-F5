package com.fc.v2.model.custom.itemflow;

import java.util.ArrayList;
import java.util.List;

/**
 * 一张申报单此刻全貌：停在第几格（顺留痕回算）、各格门槛过没过、页面格号与回算对得齐否、
 * 版次/现行/校验码。屏上带出的格次与推进方法回的不一致时，听推进方法的。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ItemFlowView {

    /** 一格所见 */
    public static class StageView {
        private int stage;
        private String stageName;
        /** 这一格门槛是否已过（顺留痕回算） */
        private boolean passed;
        /** 这一格头一遍上报留的那句（第二遍不另起一行） */
        private String note;

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

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    private String flowId;
    private String bizNo;
    private String declareNo;
    private int versionNo;
    /** 0往期 1现行（现行名录只露1） */
    private int currentFlag;
    private int roundNo;
    /** 权威当前格（顺留痕点出） */
    private int currentStage;
    private String currentStageName;
    /** 单据格子里摆的参考格号 */
    private Integer displayedStage;
    /** 参考格号与回算格对得齐否（页面只作参考，相左听回算） */
    private boolean stageMatched;
    /** 0未起 1在办 2已收口 */
    private int status;
    /** 收口说法 0未收口 1列入 2注销 3终止 */
    private int closeType;
    /** 收口当场锁档 */
    private boolean locked;
    /** 列入一刻钉下的校验码，未列入为空 */
    private String checkCode;
    private String siteNo;
    private List<StageView> stages = new ArrayList<>();

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
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

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public int getCurrentFlag() {
        return currentFlag;
    }

    public void setCurrentFlag(int currentFlag) {
        this.currentFlag = currentFlag;
    }

    public int getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(int roundNo) {
        this.roundNo = roundNo;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    public String getCurrentStageName() {
        return currentStageName;
    }

    public void setCurrentStageName(String currentStageName) {
        this.currentStageName = currentStageName;
    }

    public Integer getDisplayedStage() {
        return displayedStage;
    }

    public void setDisplayedStage(Integer displayedStage) {
        this.displayedStage = displayedStage;
    }

    public boolean isStageMatched() {
        return stageMatched;
    }

    public void setStageMatched(boolean stageMatched) {
        this.stageMatched = stageMatched;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCloseType() {
        return closeType;
    }

    public void setCloseType(int closeType) {
        this.closeType = closeType;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public List<StageView> getStages() {
        return stages;
    }

    public void setStages(List<StageView> stages) {
        this.stages = stages;
    }
}
