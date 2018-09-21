package com.netbrain.xf.flowengine.scheduler;

import org.quartz.JobDataMap;
import org.quartz.JobKey;

import java.util.Date;

public class DelayedTask {
    /**
     * The nextFireTime calculated when the task was supposed to start
     */
    private Date nextFireTimeCalcuatedAtFireTime;

    /**
     * The supposed fireTime
     */
    private Date plannedFireTime;

    private String jobId;

    private JobDataMap jobDataMap;

    private JobKey jobKey;

    public Date getNextFireTimeCalcuatedAtFireTime() {
        return nextFireTimeCalcuatedAtFireTime;
    }

    public void setNextFireTimeCalcuatedAtFireTime(Date nextFireTimeCalcuatedAtFireTime) {
        this.nextFireTimeCalcuatedAtFireTime = nextFireTimeCalcuatedAtFireTime;
    }

    public Date getPlannedFireTime() {
        return plannedFireTime;
    }

    public void setPlannedFireTime(Date plannedFireTime) {
        this.plannedFireTime = plannedFireTime;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobDataMap getJobDataMap() {
        return jobDataMap;
    }

    public void setJobDataMap(JobDataMap jobDataMap) {
        this.jobDataMap = jobDataMap;
    }

    public JobKey getJobKey() {
        return jobKey;
    }

    public void setJobKey(JobKey jobKey) {
        this.jobKey = jobKey;
    }
}
