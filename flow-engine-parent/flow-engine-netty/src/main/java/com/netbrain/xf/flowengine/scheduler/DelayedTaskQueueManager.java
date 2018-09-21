package com.netbrain.xf.flowengine.scheduler;

import com.netbrain.xf.flowengine.scheduler.services.SchedulerServicesImpl;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class DelayedTaskQueueManager {
    private static Logger logger = LogManager.getLogger(DelayedTaskQueueManager.class.getName());

    private List<DelayedTask> delayedTaskList = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    TaskController taskController;

    @Autowired
    ScheduledJobTaskRunner scheduledJobTaskRunner;

    @Autowired
    SchedulerServicesImpl schedulerServices;

    public boolean addDelayedTask(DelayedTask delayedTask) {
        return delayedTaskList.add(delayedTask);
    }

    /**
     * Try to run a delayedTask if
     * 1) there is no running taskflows for the same job id
     * 2) current time is before nextFireTime
     * 3) the same job still exists in Quartz store (in case the scheduled job is changed or deleted by user)
     *
     * @param delayedTask
     * @return true if the task can be removed from delayedTaskList
     */
    public boolean tryRun(DelayedTask delayedTask) {
        if (delayedTask != null && delayedTask.getJobId() != null) {
            String jobId = delayedTask.getJobId();
            Date nextFireTime = delayedTask.getNextFireTimeCalcuatedAtFireTime();

            // if there is no nextFireTime, try forever, otherwise try before nextFireTime
            if ((nextFireTime == null) || (nextFireTime.after(new Date()))) {
                if (taskController.hasAnyUnfinishedTaskflowsForJobId(jobId)) {
                    // still occupied, try next time
                    logger.debug("The job {} still has running taskflow, skip", jobId);
                    return false;
                } else {
                    List<JobDetail> jobDetails = schedulerServices.getJobDetails(jobId);
                    boolean foundSameJobInQuartzStore = false;
                    for (JobDetail jobDetail: jobDetails) {
                        if (jobDetail.getKey().compareTo(delayedTask.getJobKey()) == 0) {
                            foundSameJobInQuartzStore = true;
                            scheduledJobTaskRunner.startNextRun(delayedTask.getJobDataMap(), jobId);
                        }
                    }
                    logger.info("The job {} has no running tasks right now, found in job store {}", jobId, foundSameJobInQuartzStore);
                    return foundSameJobInQuartzStore;
                }
            } else {
                // we have passed the nextFireTime, no need to try any more
                logger.info("The job {} has passed nextFireTime while we are delaying current task, stop trying", jobId);
                return true;
            }
        } else {
            // no jobId on this entry, don't bother in the future
            logger.info("The job entry has no jobId, do not try any more");
            return true;
        }
    }

    public void checkAndRun() {
        synchronized (delayedTaskList) {
            delayedTaskList.removeIf(delayedTask -> (tryRun(delayedTask)));
        }
    }
}
