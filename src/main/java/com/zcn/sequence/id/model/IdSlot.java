package com.zcn.sequence.id.model;

import java.util.Date;

/**
 * @author zicung
 */
public class IdSlot {

    private int type;

    private long max;

    private int step;

    private int maxStep;

    private int stepDuration;

    private Date updateTime;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getMaxStep() {
        return maxStep;
    }

    public void setMaxStep(int maxStep) {
        this.maxStep = maxStep;
    }

    public int getStepDuration() {
        return stepDuration;
    }

    public void setStepDuration(int stepDuration) {
        this.stepDuration = stepDuration;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
