package com.netbrain.xf.flowengine.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.utility.HASupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.netbrain.xf.flowengine.scheduler.services.ISchedulerServices;

public class RefreshDbJob extends QuartzJobBean {

    private static Logger logger = LogManager.getLogger(RefreshDbJob.class.getName());
    @Autowired
    ISchedulerServices schedulerServices;

    @Autowired
    private HASupport haSupport;

    @Autowired
    private DataCenterSwitching dcSwitching;
    
    @Override
    protected void executeInternal(JobExecutionContext context)throws JobExecutionException  {
        if (haSupport.isActive() && dcSwitching.isActiveDC()) {
            logger.debug("start refreshdb");
            schedulerServices.scheduleAllJobs();
        }
        else{
            logger.debug("Noop in standby mode or inactive DC.");
        }
    }
}