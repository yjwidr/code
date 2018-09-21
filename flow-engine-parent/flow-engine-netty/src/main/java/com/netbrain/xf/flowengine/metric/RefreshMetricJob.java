package com.netbrain.xf.flowengine.metric;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbrain.xf.flowengine.gateway.AMQPTaskGateway;

public class RefreshMetricJob extends QuartzJobBean {
    private static Logger logger = LogManager.getLogger(RefreshMetricJob.class.getName());
    @Autowired
    Metrics metrics;
    @Autowired
    private DataCenterSwitching dcSwitching;
    @Value("${taskengine.metirc.path}")
    private String path;   
    @Override
    protected void executeInternal(JobExecutionContext context)throws JobExecutionException  {
        /*if(!dcSwitching.isActiveDC()) {
            logger.debug("Noop in inactive DC.");
            return;
        }*/
        HashMap<String, Long> monitorMetrics = new HashMap<String, Long>();

        monitorMetrics.put("taskengine.start.count", metrics.getAndResetStartCount());
        monitorMetrics.put("taskengine.stop.count", metrics.getAndResetStopCount());
        monitorMetrics.put("taskengine.stepdown.count", metrics.getAndResetStepdownCount());
        monitorMetrics.put("taskengine.stepup.count", metrics.getAndResetStepupCount());
        monitorMetrics.put("taskengine.dtg.stop.count", metrics.getAndResetStopCount());
        monitorMetrics.put("taskengine.dtg.query.count", metrics.getAndResetDtgQueryCount());
        monitorMetrics.put("taskengine.trigger.count", metrics.getAndResetTriggerCount());
        monitorMetrics.put("taskengine.taskflow.start.count", metrics.getAndResetTaskflowStartCount());
        monitorMetrics.put("taskengine.taskflow.end.count", metrics.getAndResetTaskflowEndCount());
        monitorMetrics.put("taskengine.task.completed.count", metrics.getAndResetTaskCompletedCount());
        monitorMetrics.put("taskengine.task.crashed.count", metrics.getAndResetTaskCrashedCount());
        monitorMetrics.put("taskengine.task.cancelled.count", metrics.getAndResetTaskCancelledCount());
        monitorMetrics.put("taskengine.task.pending.snapshot", metrics.getAndResetTaskPendingSnapshot());
        monitorMetrics.put("taskengine.task.inmemory.snapshot", metrics.getAndResetTaskInmemorySnapshot());
        monitorMetrics.put("taskengine.task.unack.snapshot", metrics.getAndResetTaskUnackSnapshot());
        monitorMetrics.put("taskengine.scheduler.skipped.count", metrics.getAndResetSchedulerSkippedCount());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(monitorMetrics);
            File file = new File(path);
            FileUtils.write(file, json, "UTF-8",false);
            logger.debug("path={} Has been written successfully",path);
        } catch (JsonProcessingException e){
            logger.error("Failed to convert to json", e);
        } catch (IOException e) {
            logger.error("Failed to write metrics file", e);
        }
    }
}