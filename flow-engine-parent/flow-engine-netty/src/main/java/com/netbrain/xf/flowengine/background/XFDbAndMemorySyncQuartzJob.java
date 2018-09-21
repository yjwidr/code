package com.netbrain.xf.flowengine.background;

import com.netbrain.xf.flowengine.dao.XFAgentRepository;
import com.netbrain.xf.flowengine.dao.XFDtgRepository;
import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskflowInMemoryRepository;
import com.netbrain.xf.flowengine.queue.ITaskQueueManager;
import com.netbrain.xf.flowengine.queue.TaskRequest;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.flowengine.utility.CommonUtil;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.utility.HASupport;
import com.netbrain.xf.flowengine.daoinmemory.XFAgentInMemoryRepository;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentMetadata;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import com.netbrain.xf.xfcommon.XFCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.Instant;
import java.util.*;

@DisallowConcurrentExecution
public class XFDbAndMemorySyncQuartzJob extends QuartzJobBean {

    private static Logger logger = LogManager.getLogger(XFDbAndMemorySyncQuartzJob.class.getSimpleName());

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private XFDtgRepository xfDtgRepository;

    @Autowired
    private XFTaskRepository taskRepository;

    @Autowired
    private XFTaskInMemoryRepository taskInMemoryRepository;

    @Autowired
    private XFTaskflowRepository taskflowRepository;

    @Autowired
    private XFTaskflowInMemoryRepository taskflowInMemoryRepository;

    @Autowired
    private XFAgentRepository xfAgentRepository;

    @Autowired
    XFAgentInMemoryRepository xfAgentInMemoryRepository;

    @Autowired
    private ITaskQueueManager taskQueueManager;

    @Autowired
    private HASupport haSupport;

    @Autowired
    private DataCenterSwitching dcSwitching;

    @Autowired
    private TaskController taskController;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        if(haSupport.isActive() && dcSwitching.isActiveDC()) {
            PerformXFDbAndMemorySyncQuartzJob();
        }
        else{
            logger.debug("Noop in standby mode or inactive DC.");
        }
    }

    private List<XFTask> performXFTaskSyncWithDBFromMemory()
    {
        try
        {
            List<XFTask> syncXFTasks = new ArrayList<XFTask>();
            Set<Map.Entry<String, XFTask>> allXFTasks = taskInMemoryRepository.getAllXFTasks();
            for (Map.Entry<String, XFTask> entry: allXFTasks) {
                XFTask aXFTask = entry.getValue();
                if (aXFTask == null ) {
                    continue;
                }

                Optional<XFTask> taskInDBOpt = this.taskRepository.findById(aXFTask.getId());
                if (taskInDBOpt.isPresent() == false)
                {
                    try {
                        this.taskRepository.save(aXFTask);
                        syncXFTasks.add(aXFTask);
                    }
                    catch (Exception saveEx)
                    {
                        logger.warn("performSyncWithDB taskRepository.save failed exception for taskflowId=" + aXFTask.getId() , saveEx);
                    }
                }
                else
                {
                    XFTask taskInDB = taskInDBOpt.get();
                    this.taskInMemoryRepository.updateXFAgentInfo(aXFTask, taskInDB.getXFAgentProcessId(), aXFTask.getWorkerMachineName());
                    if (!taskInDB.getStatusIsFinal() && taskInDB.getTaskStatus() < aXFTask.getTaskStatus())
                    {
                        if(this.taskRepository.updateStatus(aXFTask, aXFTask.getTaskStatus(), "", null)){
                            syncXFTasks.add(aXFTask);
                        }
                    }
                    else if (!aXFTask.getStatusIsFinal() && aXFTask.getTaskStatus() < taskInDB.getTaskStatus())
                    {
                        if(taskInDB.getStatusIsFinal()){
                            String finalReason = "XFDbAndMemorySyncQuartzJob detected task has complated";
                            if(this.taskController.processTaskEndMessage(taskInDB.getId(), taskInDB.getTaskStatus(), finalReason)){
                                syncXFTasks.add(taskInDB);
                            }
                        } else{
                            taskInMemoryRepository.upsertXFTask_notUpdateDB(taskInDB.getId(), taskInDB);
                        }
                    }
                    else{
                        syncXFTasks.add(aXFTask);
                    }
                }
            }

            return syncXFTasks;
        }
        catch (Exception e)
        {
            logger.warn("performXFTaskSyncWithDBFromMemory failed with exception:" , e);
        }

        return null;
    }

    private void performXFTaskSyncWithMemoryFromDB()
    {
        int currTaskCount = this.taskInMemoryRepository.getXFTaskCount();
        // The taskgateway and triggerReceiver might be also creating tasks to TaskQueue, to reduce the risk of over sending task, we send at most 100 at a time.
        int nLimit = Math.min(100,  ((int)commonUtil.maxTaskCountLimitInConfig - currTaskCount));
        if (nLimit > 0) {
            List<XFTask> lstXFTaskInDB = this.taskRepository.findTopNUnfinishedTasksByTaskPriorityAndSubmitTime(nLimit);
            for (XFTask taskInDB : lstXFTaskInDB) {
                String taskId = taskInDB.getId();
                XFTask taskInMem = this.taskInMemoryRepository.getXFTask(taskId);
                if (taskInMem == null) {
                    // not in memory, need to sync it from DB to memory
                    this.taskInMemoryRepository.upsertXFTask_notUpdateDB(taskId, taskInDB);

                    // for the tasks prior to RUNNING status, enqueue it to TaskQueueManager if this is an active TaskEngine instance
                    if (taskInDB.getTaskStatus() == XFCommon.TASKSTATUS_Started || taskInDB.getTaskStatus() == XFCommon.TASKSTATUS_Scheduled) {
                        taskQueueManager.enqueue(new TaskRequest(taskInDB, null));
                        logger.debug("enqueue task, selfTaskId=" + taskId + " for sync from db to memory");
                    } else if (taskInDB.getTaskStatus() == XFCommon.TASKSTATUS_Running) {
                        String machineName = taskInDB.getWorkerMachineName();
                        XFAgent xfagent = xfAgentInMemoryRepository.GetOneXFAgent(machineName);
                        //add a retired XFAgent
                        if (xfagent == null) {
                            Optional<XFAgent> xfAgentOpt = this.xfAgentRepository.findById(machineName);
                            if (xfAgentOpt.isPresent()) {
                                XFAgent xfagentmodelFromDB = xfAgentOpt.get();
                                xfagentmodelFromDB.setRetired(true);

                                XFAgentMetadata agentMetadata = new XFAgentMetadata();
                                agentMetadata.setServerName(machineName);
                                agentMetadata.setUniqIdForEachUpdate(xfagentmodelFromDB.getUniqIdForEachUpdate());
                                agentMetadata.setFirsttimeReceivedThisUniqId(Instant.now());
                                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(xfagentmodelFromDB);
                                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(agentMetadata);
                                logger.info("Add a retired XFAgent for serverName {} and task {}", machineName, taskInDB.getSelfTaskId());

                            } else {
                                String finalReason = "XFDbAndMemorySyncQuartzJob detected unknown XFAgent for serverName " + machineName;
                                this.taskController.processTaskEndMessage(taskInDB.getId(), XFCommon.TASKSTATUS_CompletedCrash, finalReason);
                                logger.warn(finalReason);
                            }
                        }

                        xfagent = xfAgentInMemoryRepository.GetOneXFAgent(machineName);
                        if (xfagent != null) {

                            if (taskInDB.getXFAgentProcessId() != -1 && taskInDB.getXFAgentProcessId() != xfagent.getXfAgentProcessId()) {
                                String finalReason = "XFDbAndMemorySyncQuartzJob detected task " + taskId
                                        + " its xfAgentProcessId " + taskInDB.getXFAgentProcessId() + " does not match that( "
                                        + xfagent.getXfAgentProcessId()
                                        + ") of RMAgent on serverName " + machineName;
                                this.taskController.processTaskEndMessage(taskInDB.getId(), XFCommon.TASKSTATUS_CompletedCrash, finalReason);
                                logger.warn(finalReason);
                            }
                        }
                    }
                } else {
                    // even if it exists in memory, if the one in DB has a newer status, sync it to memory
                    if (!taskInMem.getStatusIsFinal() && taskInMem.getTaskStatus() < taskInDB.getTaskStatus()) {
                        this.taskInMemoryRepository.upsertXFTask_notUpdateDB(taskId, taskInDB);
                    }

                    taskInMem = this.taskInMemoryRepository.getXFTask(taskId);
                    if (taskInMem.getTaskStatus() == XFCommon.TASKSTATUS_Running) {
                        String machineName = taskInMem.getWorkerMachineName();
                        XFAgent xfagent = xfAgentInMemoryRepository.GetOneXFAgent(machineName);
                        //add a retired XFAgent
                        if (xfagent == null) {
                            Optional<XFAgent> xfAgentOpt = this.xfAgentRepository.findById(machineName);
                            if (xfAgentOpt.isPresent()) {
                                XFAgent xfagentmodelFromDB = xfAgentOpt.get();
                                xfagentmodelFromDB.setRetired(true);

                                XFAgentMetadata agentMetadata = new XFAgentMetadata();
                                agentMetadata.setServerName(machineName);
                                agentMetadata.setUniqIdForEachUpdate(xfagentmodelFromDB.getUniqIdForEachUpdate());
                                agentMetadata.setFirsttimeReceivedThisUniqId(Instant.now());
                                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(xfagentmodelFromDB);
                                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(agentMetadata);
                                logger.info("Add a retired XFAgent for serverName {} and task {}", machineName, taskInMem.getSelfTaskId());
                            } else {
                                String finalReason = "XFDbAndMemorySyncQuartzJob detected unknown XFAgent for serverName " + machineName;
                                this.taskController.processTaskEndMessage(taskInMem.getId(), XFCommon.TASKSTATUS_CompletedCrash, finalReason);
                                logger.warn(finalReason);
                            }
                        }

                        xfagent = xfAgentInMemoryRepository.GetOneXFAgent(machineName);
                        if (xfagent != null) {

                            if (taskInDB.getXFAgentProcessId() != -1 && taskInDB.getXFAgentProcessId() != xfagent.getXfAgentProcessId()) {
                                String finalReason = "XFDbAndMemorySyncQuartzJob detected task " + taskId
                                        + " its xfAgentProcessId " + taskInDB.getXFAgentProcessId() + " does not match that( "
                                        + xfagent.getXfAgentProcessId()
                                        + ") of RMAgent on serverName " + machineName;
                                this.taskController.processTaskEndMessage(taskInDB.getId(), XFCommon.TASKSTATUS_CompletedCrash, finalReason);
                                logger.warn(finalReason);
                            }
                        }
                    }
                }
            }
        }
    }

    private List<XFTaskflow> performXFTaskflowSyncWithDBFromMemory()
    {
        try
        {
            List<XFTaskflow> syncXFTaskflows = new ArrayList<XFTaskflow>();
            Set<Map.Entry<String, XFTaskflow>> allXFTaskflows = taskflowInMemoryRepository.getAllXFTaskflows();
            for (Map.Entry<String, XFTaskflow> entry: allXFTaskflows) {
                XFTaskflow aXFTaskflow = entry.getValue();
                if (aXFTaskflow == null ) {
                    continue;
                }

                Optional<XFTaskflow> taskflowInDBOpt = this.taskflowRepository.findById(aXFTaskflow.getId());
                if (taskflowInDBOpt.isPresent() == false)
                {
                    try {
                        this.taskflowRepository.save(aXFTaskflow);
                        syncXFTaskflows.add(aXFTaskflow);
                    }
                    catch (Exception saveEx)
                    {
                        logger.warn("performSyncWithDB taskflowRepository.save failed exception for taskflowId=" + aXFTaskflow.getId() , saveEx);
                    }
                }
                else
                {
                    XFTaskflow taskflowInDB = taskflowInDBOpt.get();
                    if (!taskflowInDB.getStatusIsFinal() && taskflowInDB.getStatus() < aXFTaskflow.getStatus()) {
                        //xftodo, we should only update the status, but we don't have the api now, enhance it later if necessary
                        this.taskflowRepository.save(aXFTaskflow);
                        syncXFTaskflows.add(aXFTaskflow);
                    } else if (!aXFTaskflow.getStatusIsFinal() && aXFTaskflow.getStatus() < taskflowInDB.getStatus()) {
                        if(taskflowInDB.getStatusIsFinal()){
                            if(this.taskController.CalculateTasklowStatus(taskflowInDB.getId(), true) >= XFCommon.TASKFLOWSTATUS_CompletedNormally){
                                this.taskController.notifyTaskflowFinish(taskflowInDB);
                                syncXFTaskflows.add(taskflowInDB);
                                logger.info("notify taskflow finished, taskFlowId=" + taskflowInDB.getId() + " for sync from memory to db");
                            }
                        } else if(this.taskflowInMemoryRepository.upsertXFTaskflow_notUpdateDB(taskflowInDB.getId(), taskflowInDB)){
                            syncXFTaskflows.add(taskflowInDB);
                        }
                    } else{
                        syncXFTaskflows.add(aXFTaskflow);
                    }
                }
            }

            return syncXFTaskflows;
        }
        catch (Exception e)
        {
            logger.warn("performXFTaskflowSyncWithDBFromMemory failed with exception:" , e);
        }

        return null;
    }

    private void performXFTaskflowSyncWithMemoryFromDB()
    {
        List<XFTaskflow> lstTaskflowInDB = this.taskflowRepository.findAllUnfinishedTaskflow();
        for (XFTaskflow taskflowInDB : lstTaskflowInDB)
        {
            String taskflowId = taskflowInDB.getId();
            Optional<XFTaskflow> xfTaskflowOpt = this.taskflowInMemoryRepository.findById(taskflowId, false, false);

            if (xfTaskflowOpt.isPresent() == false)
            {
                // not in memory, need to sync it from DB to memory
                this.taskflowInMemoryRepository.upsertXFTaskflow_notUpdateDB(taskflowId, taskflowInDB);
                if(this.taskController.CalculateTasklowStatus(taskflowInDB.getId(), true) >= XFCommon.TASKFLOWSTATUS_CompletedNormally){
                    this.taskController.notifyTaskflowFinish(taskflowInDB);
                    logger.info("notify taskflow finished, taskFlowId=" + taskflowInDB.getId() + " for sync from db to memory");
                }
            }
            else
            {
                XFTaskflow taskflowInMem = xfTaskflowOpt.get();
                // even if it exiss in memory, if the one in DB has a newer status, sync it to memory
                if (!taskflowInMem.getStatusIsFinal() && taskflowInMem.getStatus() < taskflowInDB.getStatus())
                {
                    this.taskflowInMemoryRepository.upsertXFTaskflow_notUpdateDB(taskflowId, taskflowInDB);
                    if(this.taskController.CalculateTasklowStatus(taskflowId, true) >= XFCommon.TASKFLOWSTATUS_CompletedNormally){
                        this.taskController.notifyTaskflowFinish(taskflowInDB);
                        logger.info("notify taskflow finished, taskFlowId=" + taskflowInDB.getId() + " for sync from db to memory");
                    }
                }
            }
        }
    }

    private void PerformXFDbAndMemorySyncQuartzJob()
    {
        try {
            logger.debug("Flowengine is performaing XFDbAndMemorySyncQuartzJob.");

            // things to do 1, sync task and taskflow from memory to db;
            List<XFTask> syncXFTasks = performXFTaskSyncWithDBFromMemory();
            List<XFTaskflow> syncXFTaskflows = performXFTaskflowSyncWithDBFromMemory();

            // things to do 2, sync task and taskflow from db to memory
            performXFTaskSyncWithMemoryFromDB();
            performXFTaskflowSyncWithMemoryFromDB();

            // things to do 3, delete all finished task and taskflow
            if(syncXFTasks != null){
                for(XFTask xfTask : syncXFTasks){
                    if(xfTask != null && xfTask.getStatusIsFinal()){
                        taskInMemoryRepository.deleteXFTask(xfTask.getId());
                    }
                }
            }
            if(syncXFTaskflows != null){
                for(XFTaskflow xfTaskflow : syncXFTaskflows){
                    if(xfTaskflow != null && xfTaskflow.getStatusIsFinal()){
                        taskflowInMemoryRepository.deleteXFTaskflow(xfTaskflow.getId());
                    }
                }
            }
        }
        catch (Exception ex)
        {
            logger.warn("XFDbAndMemorySyncQuartzJob exception: ", ex);
        }
    }
}
