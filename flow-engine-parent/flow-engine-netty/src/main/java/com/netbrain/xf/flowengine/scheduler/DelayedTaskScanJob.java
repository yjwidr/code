package com.netbrain.xf.flowengine.scheduler;

import com.netbrain.xf.flowengine.scheduler.services.ISchedulerServices;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.utility.HASupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DelayedTaskScanJob extends QuartzJobBean {
    private static Logger logger = LogManager.getLogger(DelayedTaskScanJob.class.getName());
    @Autowired
    DelayedTaskQueueManager delayedTaskQueueManager;

    @Autowired
    private HASupport haSupport;

    @Autowired
    private DataCenterSwitching dcSwitching;

    @Override
    protected void executeInternal(JobExecutionContext context)throws JobExecutionException {
        if (haSupport.isActive() && dcSwitching.isActiveDC()) {
            delayedTaskQueueManager.checkAndRun();
        }
        else{
            logger.debug("Noop in standby mode or inactive DC.");
        }
    }
}
