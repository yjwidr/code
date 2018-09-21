package com.netbrain.xf.flowengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.netbrain.xf.flowengine.scheduler.TaskJob;

public class Test {
    private static Logger logger = LogManager.getLogger(Test.class.getName());
    Scheduler  scheduler =null;
    public Scheduler getScheduler() throws SchedulerException {
        if(this.scheduler==null) {
            this.scheduler= StdSchedulerFactory.getDefaultScheduler();
        }  
        return this.scheduler;
    }
   
    public boolean runNow(JobDataMap jobDataMap) throws SchedulerException {
        
        JobDetail jobDetail = null;
        String strJobKey = null;
        StringBuilder sb = new StringBuilder();
        String jobId = jobDataMap.getString("jobId");
        String frequency = jobDataMap.getString("frequency");
        String mtl = "-";
        JobKey jobKey = null;
        try {
            sb.append(jobId).append(mtl).append(frequency);
            if (jobDataMap.containsKey("nodelete")) {
                sb.append(mtl).append("nodelete");
            }
            strJobKey = sb.toString();
            jobKey = new JobKey(strJobKey, jobId);
            jobDetail = JobBuilder.newJob(TaskJob.class).withIdentity(jobKey).storeDurably().usingJobData(jobDataMap)
                    .build();
            if (this.scheduler.checkExists(jobDetail.getKey())) {
                if (!jobDataMap.containsKey("nodelete")) {
                    this.scheduler.deleteJob(jobDetail.getKey());
                } else {
                    logger.debug("jobkey is===={}", jobDetail.getKey().getName());
                    return false;
                }
            }
            this.scheduler.addJob(jobDetail, true);
            this.scheduler.triggerJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            logger.error("Failed to schedule jobid: " + jobId + ", jobKey: " + strJobKey, e);
            return false;
        }
    }

    public static void main(String[] args) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", "aaaaaaaaaaaa");
        jobDataMap.put("frequency", "byDay");
        Test test = new Test();
        test.getScheduler();
        while(true) {
            test.runNow(jobDataMap);
        }
        
    }
}
