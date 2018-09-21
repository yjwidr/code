package com.netbrain.xf.flowengine.background;

import com.netbrain.xf.flowengine.daoinmemory.XFAgentInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskflowInMemoryRepository;
import com.netbrain.xf.flowengine.queue.TaskQueueManagerImpl;
import com.netbrain.xf.flowengine.queue.TaskRequest;
import com.netbrain.xf.flowengine.scheduler.services.SchedulerServicesImpl;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@DisallowConcurrentExecution
public class StatusReporter extends QuartzJobBean {
    private static Logger logger = LogManager.getLogger(StatusReporter.class.getSimpleName());

    @Autowired
    TaskQueueManagerImpl taskQueueManager;

    @Autowired
    SchedulerServicesImpl schedulerServices;

    @Autowired
    XFTaskInMemoryRepository xfTaskInMemoryRepository;

    @Autowired
    XFTaskflowInMemoryRepository xfTaskflowInMemoryRepository;

    @Autowired
    XFAgentInMemoryRepository xfAgentInMemoryRepository;

    @Autowired
    private DataCenterSwitching dcSwitching;

    private void printOutQueuedTasks() {
        boolean hasRecord = false;
        for (TaskRequest taskRequest: taskQueueManager.exportQueueElements()) {
            if (!hasRecord) {
                logger.debug("------------exporting all tasks in the pending queue------------");
                hasRecord = true;
            }
            if (taskRequest != null && taskRequest.getXfTask() != null) {
                logger.debug(taskRequest.getXfTask().toSummaryString());
            }
        }
    }

    private void printOutInMemoryTasks() {
        boolean hasRecord = false;
        for (Map.Entry<String, XFTask> entry : xfTaskInMemoryRepository.getAllXFTasks()) {
            if (!hasRecord) {
                logger.debug("------------exporting all tasks in the memory repository------------");
                hasRecord = true;
            }
            if (entry != null && entry.getValue() != null) {
                logger.debug(entry.getValue().toSummaryString());
            }
        }

        for (Map.Entry<String, XFTaskflow> entry: xfTaskflowInMemoryRepository.getAllXFTaskflows()) {
            if (entry != null && entry.getValue() != null) {
                logger.debug(entry.getValue().toString());
            }
        }
    }

    private void printOutScheduledTasks() {
        logger.debug("------------exporting all scheduled jobs------------");
        schedulerServices.loopScheduler(logger);
    }

    private void printOutWorkerServers() {
        logger.debug("------------exporting all worker servers------------");
        for (XFAgent xfAgent: xfAgentInMemoryRepository.getXFAgentServerHashMap().values()) {
            if (xfAgent != null) {
                logger.debug(xfAgent.toString());
            }
        }
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // See log4j2.xml for the location and level of this logger
            printOutQueuedTasks();

            printOutInMemoryTasks();

            printOutScheduledTasks();

            printOutWorkerServers();
    }
}
