package com.netbrain.xf.flowengine.scheduler.services;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;

import org.quartz.JobDataMap;

public interface ISchedulerServices {
    public Map<String, JobDataMap> getJobDataMap();
    public boolean deleteJob(String jobId);
    public boolean runNow(String jobId);
    public void makeScheduler(JobDataMap jobDataMap);
    public List<String> loopScheduler(Logger logger);
    public String getLastTimeNextTime();
    public void cleanJobs(List<String> preJobIds);

    public void scheduleAllJobs();
    public void addScheduledJob(String jobId);
}
