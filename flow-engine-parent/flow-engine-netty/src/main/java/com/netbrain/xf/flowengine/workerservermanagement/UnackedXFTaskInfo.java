package com.netbrain.xf.flowengine.workerservermanagement;

import com.netbrain.xf.model.XFTask;

import java.time.Instant;

public class UnackedXFTaskInfo {

    public UnackedXFTaskInfo(XFTask xfTask, Instant timesttampPublish, int resendTimes) {
        this.xfTask = xfTask;
        this.timesttampPublish = timesttampPublish;
        this.resendTimes = resendTimes;
    }

    private UnackedXFTaskInfo()
    {

    }

    public XFTask getXfTask() {
        return xfTask;
    }

    public void setXfTask(XFTask xfTask) {
        this.xfTask = xfTask;
    }

    public Instant getTimesttampPublish() {
        return timesttampPublish;
    }

    public void setTimesttampPublish(Instant timesttampPublish) {
        this.timesttampPublish = timesttampPublish;
    }

    public int getResendTimes() {
        return resendTimes;
    }

    public void setResendTimes(int resendTimes) {
        this.resendTimes = resendTimes;
    }

    private XFTask xfTask;
    private Instant timesttampPublish;
    private int resendTimes = 0;
}
