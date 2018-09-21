package com.netbrain.xf.flowengine.scheduler;

import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.netbrain.xf.flowengine.metric.Metrics;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.utility.HASupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.netbrain.xf.flowengine.taskcontroller.TaskController;

import static com.mongodb.client.model.Filters.eq;

public class TaskJob extends QuartzJobBean {
    private static Logger logger = LogManager.getLogger(TaskJob.class.getName());
    @Autowired
    TaskController taskController;

    @Autowired
    private HASupport haSupport;

    @Autowired
    private DataCenterSwitching dcSwitching;

    @Autowired
    private ScheduledJobTaskRunner scheduledJobTaskRunner;

    @Autowired
    private DelayedTaskQueueManager delayedTaskQueueManager;

    @Autowired
    private Metrics metrics;

    @Override
    protected void executeInternal(JobExecutionContext context)
            throws JobExecutionException {
        Lock lock = new ReentrantLock();  
        lock.lock();
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        JobKey jobKey = context.getJobDetail().getKey();
        if(haSupport.isActive() && dcSwitching.isActiveDC()) {
            String jobId = jobDataMap.getString("jobId");
            if (jobId != null) {
                if ((jobDataMap.containsKey("stopOldTaskAtNextRun") && jobDataMap.getBoolean("stopOldTaskAtNextRun"))) {
                    if (taskController.hasAnyUnfinishedTaskflowsForJobId(jobId)) {
                        logger.info("Delaying a scheduled task since there are unfinished taskflows for JobID {}", jobId);
                        DelayedTask delayedTask = new DelayedTask();
                        delayedTask.setJobId(jobId);
                        delayedTask.setPlannedFireTime(context.getFireTime());
                        delayedTask.setNextFireTimeCalcuatedAtFireTime(context.getNextFireTime());
                        delayedTask.setJobDataMap(jobDataMap);
                        delayedTask.setJobKey(jobKey);
                        delayedTaskQueueManager.addDelayedTask(delayedTask);
                    } else {
                        scheduledJobTaskRunner.startNextRun(jobDataMap, jobId);
                    }
                } else {
                    if (!taskController.hasAnyUnfinishedTaskflowsForJobId(jobId)) {
                        scheduledJobTaskRunner.startNextRun(jobDataMap, jobId);
                    } else {
                        logger.info("Skip the current schedule since the scheduled job {} has unfinished tasks", jobId);
                        metrics.addSchedulerSkippedCount(1);
                    }
                }
            } else {
                logger.warn("Cannot run a task due to missing jobId");
            }

            for(Entry<String,Object> entry:jobDataMap.entrySet()) {
                logger.debug("key={},value={}",entry.getKey(),entry.getValue());
            }
        }else {
            logger.debug("not leader or in inactive DC.");
        }
        lock.unlock();
    }

}