package com.netbrain.xf.flowengine.taskcontroller;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.netbrain.ngsystem.model.FrontServerController;
import com.netbrain.xf.flowengine.amqp.PublisherWithRetry;
import com.netbrain.xf.flowengine.dao.XFDtgRepository;
import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFAgentInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskflowInMemoryRepository;
import com.netbrain.xf.flowengine.fscclient.FSCRepository;
import com.netbrain.xf.flowengine.fscclient.FSCRequest;
import com.netbrain.xf.flowengine.fscclient.NetlibClient;
import com.netbrain.xf.flowengine.gateway.AMQPTaskGateway;
import com.netbrain.xf.flowengine.metric.Metrics;
import com.netbrain.xf.flowengine.queue.ITaskQueueManager;
import com.netbrain.xf.flowengine.queue.TaskRequest;
import com.netbrain.xf.flowengine.utility.CommonUtil;
import com.netbrain.xf.flowengine.utility.XFAgentSelectionAlgorithm;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentMetadata;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFDtg;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import com.netbrain.xf.model.util.XFTaskUtil;
import com.netbrain.xf.xfcommon.XFCommon;
import com.rabbitmq.client.AMQP;

import io.netty.util.internal.StringUtil;

/**
 * XFTaskflow and XFTask status and life cycle management
 */
@Component
public class TaskController {

    public static final int FLOWSTATUS_FORCE_REFRESH_TIME_INTERVAL = 10;
    private static Logger logger = LogManager.getLogger(TaskController.class.getSimpleName());

    private volatile Instant timestampLastTaskSelectXFAgent = Instant.EPOCH;

    public static final int XFAGENT_TASK_ROUNDROBIN_INTERVAL_IN_SECONDS = 5;

    // defined by C# System.Threading.Timeout.Infinite, this means we will wait forever
    public static final int TIMEOUT_INFINITE = -1;

    @Value("${workerserver.queue.name.version}")
    private int workerQueueNameVersion;

    @Value("${workerserver.unack.count.limit}")
    private int workerUnackLimit;

    @Value("${taskengine.internal.selectbestworkerdetail}")
    private boolean showselectbestworkerdetail;

    @Value("${workerserver.crash.detection.missing.heartbeat.count}")
    private long missingHeartbeatCountInConfig;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private XFTaskInMemoryRepository taskInMemoryRepository;

    @Autowired
    private XFTaskRepository taskRepository;

    @Autowired
    private XFTaskflowInMemoryRepository taskflowInMemoryRepository;
    
    @Autowired
    private XFTaskflowRepository taskflowRepository;

    @Autowired
    private XFDtgRepository dtgRepository;

    @Autowired
    private FSCRepository fscRepository;

    @Autowired
    XFAgentInMemoryRepository xfAgentInMemoryRepository;

    @Autowired
    private ITaskQueueManager taskQueueManager;

    @Value("${workerserver.selection.ram.high.watermark}")
    private long ramHighWatermark;

    @Value("${workerserver.concurrent.control.tasks.count}")
    private long concurTaskLimitPerWorker;

    @Value("${workerserver.concurrent.control.task.types}")
    private String concurTaskLimitedTypes;

    private List<String> concurTaskLimitedTypeArray;

    @Value("${workerserver.selection.ram.per.task.estimate}")
    private long perTaskPhysicalMemMB;

    @Autowired
    private PublisherWithRetry publisher;
    
    @Autowired
    private Metrics metrics;

    protected void setPublisher(PublisherWithRetry publisher) {
        this.publisher = publisher;
    }

    public Optional<XFTaskflow> getTaskflow(String taskflowId) {
        return taskflowInMemoryRepository.findById(taskflowId, true, true);
    }

    @PostConstruct
    private void init() {
        concurTaskLimitedTypeArray = Arrays.asList(concurTaskLimitedTypes.split(","));
    }

    public Optional<XFTask> getTask(String taskId) {
        Optional<XFTask> retXFTaskOpt = this.taskInMemoryRepository.findById(taskId);
        // if find it in memory, return it directly, otherwise try to see if it exists in DB
        if (retXFTaskOpt.isPresent() == true)
        {
            return  retXFTaskOpt;
        }
        else
        {
            logger.warn("getTask failed to find XFTask in DB for selfTaskId " + taskId);
            retXFTaskOpt = Optional.empty();
        }

        return retXFTaskOpt;
    }

    public XFCommon.XFAgentSelectionResult selectBestXFAgent(XFTask taskToSend) {
        return selectBestXFAgent(taskToSend, true);
    }

    /**
     * select the best XFAgent based on different algorithm.
     * The XFAgent information is read from DB, which is periodically updated by XFAgent process
     *
     * @param taskToSend
     * @param excludeOverloadedWorker if set to true, the overloaded servers cannot be used
     * @return
     */
    public XFCommon.XFAgentSelectionResult selectBestXFAgent(XFTask taskToSend, boolean excludeOverloadedWorker)
    {
        XFCommon.XFAgentSelectionResult ret = new XFCommon.XFAgentSelectionResult();
        int taskPrio = taskToSend.getTaskPriority();
        String taskId = taskToSend.getId();
        //Use the in memory repository as cache
        List<XFAgent> listAgentsInMemory = this.xfAgentInMemoryRepository.GetAllInMemoryXFAgent();
        int origSize = listAgentsInMemory.size();
        if (origSize == 0)
        {
            ret.resultCode = XFCommon.XFAgentSelectionResultCode.RESULT_NO_XFAGENT;
            String taskStr = taskToSend.toString();
            if (!taskToSend.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_NO_AGENT)) {
                logger.warn("selectBestXFAgent can not find an XFAgent to execute this task: " + taskStr);
            } else {
                logger.debug("selectBestXFAgent can not find an XFAgent to execute this task: " + taskStr);
            }

            return ret;
        }

        listAgentsInMemory.removeIf((XFAgent agentInMem) ->{
            String xfagentName = agentInMem.getServerName();
            if(agentInMem.isRetired()){
                if (this.showselectbestworkerdetail)
                {
                    String strDetail = String.format("When routing taskid %s, %s will not be selected because it is retired. ", taskId, xfagentName);
                    logger.info(strDetail);
                }
                return true;
            }

            XFAgentMetadata metadata = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(xfagentName);

            // If the XFAgent has never performed an update, do not use this XFAgent yet.
            int xfagent_pid = agentInMem.getXfAgentProcessId();
            if ( xfagent_pid <= 0)
            {
                if (this.showselectbestworkerdetail)
                {
                    String strDetail = String.format("When routing taskid %s, %s will not be selected because our record shows its processId %d is invalid. ", taskId, xfagentName, xfagent_pid);
                    logger.info(strDetail);
                }
                return true;
            }

            //xftodo: this is the mongodb-detected-dead, add the OR relationship with rabbitmq-detected-dead
            if (metadata.isBlacklisted() == true)
            {
                if (this.showselectbestworkerdetail)
                {
                    String strDetail = String.format("When routing taskid %s, %s will not be selected because our record shows currently it is in blacklist. ", taskId, xfagentName);
                    logger.info(strDetail);
                }
                return true;
            }

            // unack window is full
            if ((workerUnackLimit >= 0) && metadata.getUnackedXFTaskCount() >= workerUnackLimit) {
                if (this.showselectbestworkerdetail) {
                    String strDetail = String.format("When routing taskid %s, %s will not be selected because it has %d unacked tasks. ",
                            taskId, xfagentName, metadata.getUnackedXFTaskCount());
                    logger.info(strDetail);
                }
                return true;
            }
            return false;

        });

        List<XFAgent> listAgents = listAgentsInMemory;

        int nSize = listAgents.size();
        if (nSize == 0 )
        {
            ret.resultCode = XFCommon.XFAgentSelectionResultCode.RESULT_NO_AVAILABLE_XFAGENT;
            String taskStr = taskToSend.toString();
            if (!taskToSend.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_NO_AVAIL_AGENT)) {
                logger.warn("When routing taskid {}, although there are {} configured XFAgernt server, selectBestXFAgent can not find an available one to execute this task {}.", taskId, origSize, taskStr);
            } else {
                logger.debug("When routing taskid {}, although there are {} configured XFAgernt server, selectBestXFAgent can not find an available one to execute this task {}.", taskId, origSize, taskStr);
            }
            return ret;
        }

        // if missing heartbeat more than certain number of times, and there is a better one than it, we will discriminate this server
        // See ENG-41065 for details
        if (listAgents.size() > 1 )
        {
            long minMissingCount = 0;
            for(XFAgent oneXFAgent : listAgents)
            {
                String xfagentName = oneXFAgent.getServerName();
                XFAgentMetadata metadata = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(xfagentName);
                if (metadata == null) { continue;}

                minMissingCount = Math.min(minMissingCount, metadata.getMissingHeartbeatCount());
            }

            // the discrimination threshold is one third of the total missing heartbeat count for marking a server as dead
            long MissingCountDiscriminationThreshold = Math.max(2, missingHeartbeatCountInConfig/3);
            Iterator<XFAgent> iter = listAgentsInMemory.iterator();
            while (iter.hasNext())
            {
                XFAgent oneXFAgent = iter.next();
                String xfagentName = oneXFAgent.getServerName();
                XFAgentMetadata metadata = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(xfagentName);
                if (metadata == null) { continue;}

                long currMissingCount = metadata.getMissingHeartbeatCount();
                if (currMissingCount - minMissingCount > MissingCountDiscriminationThreshold) {

                    iter.remove();

                    if (this.showselectbestworkerdetail)
                    {
                        String strDetail =
                                String.format("When routing taskid %s, %s will not be selected because its missing heartbeat count is %d, while the minimum among its peers is %d . ", taskId, xfagentName, currMissingCount, minMissingCount);
                        logger.info(strDetail);
                    }
                }
            }
        }

        if (excludeOverloadedWorker) {
            boolean bRemovedBecauseOfPerPrioOverload = false;
            if (taskPrio == XFCommon.TASK_PRIORITY_SUPER) {
                bRemovedBecauseOfPerPrioOverload = listAgents.removeIf((XFAgent agent) -> {
                    boolean bRemove = agent.isP1IsOverloaded() || agent.isServerIsOverloaded();
                    if (bRemove && this.showselectbestworkerdetail) {
                        String strDetail = String.format("When routing taskid %s of priority %d, %s will not be selected because that server is overloaded for that priority. ", taskId, taskPrio, agent.getServerName());
                        logger.info(strDetail);
                    }
                    return bRemove;
                });
            } else if (taskPrio == XFCommon.TASK_PRIORITY_HIGH) {
                bRemovedBecauseOfPerPrioOverload = listAgents.removeIf((XFAgent agent) -> {
                    boolean bRemove = agent.isP2IsOverloaded() == true || agent.isServerIsOverloaded();
                    if (bRemove && this.showselectbestworkerdetail) {
                        String strDetail = String.format("When routing taskid %s of priority %d, %s will not be selected because that server is overloaded for that priority. ", taskId, taskPrio, agent.getServerName());
                        logger.info(strDetail);
                    }
                    return bRemove;
                });
            } else // treated as if (taskPrio == XFCommon.TASK_PRIORITY_LOW)
            {
                bRemovedBecauseOfPerPrioOverload = listAgents.removeIf((XFAgent agent) -> {
                    boolean bRemove = agent.isP3IsOverloaded() == true || agent.isServerIsOverloaded();
                    if (bRemove && this.showselectbestworkerdetail) {
                        String strDetail = String.format("When routing taskid %s of priority %d, %s will not be selected because that server is overloaded for that priority. ", taskId, taskPrio, agent.getServerName());
                        logger.info(strDetail);
                    }
                    return bRemove;
                });
            }
            nSize = listAgents.size();
            if (nSize == 0) {
                ret.resultCode = XFCommon.XFAgentSelectionResultCode.RESULT_NO_AVAILABLE_XFAGENT;
                String taskStr = taskToSend.toString();
                if (!taskToSend.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_NO_AVAIL_AGENT)) {
                    logger.warn("When routing priority {} taskid {}, although there are {} configured XFAgernt server, selectBestXFAgent can not find an available one to execute this task {}.", taskPrio, taskId, origSize, taskStr);
                } else {
                    logger.debug("When routing priority {} taskid {}, although there are {} configured XFAgernt server, selectBestXFAgent can not find an available one to execute this task {}.", taskPrio, taskId, origSize, taskStr);
                }
                return ret;
            }
        }

        // Check if there are too many running tasks for a given type on a worker
        if (concurTaskLimitedTypeArray.contains(taskToSend.getTaskType())) {
            listAgents.removeIf((XFAgent agent) -> {
                long assignedTaskCount = taskRepository.countAssignedTasksByWorker(agent.getServerName(), concurTaskLimitedTypeArray);
                boolean bRemove = (assignedTaskCount >= concurTaskLimitPerWorker);
                if (bRemove && this.showselectbestworkerdetail)
                {
                    String strDetail = String.format("When routing taskid %s of priority %d, %s will not be selected because assgined task count %d >= limit %d. ", taskId,taskPrio, agent.getServerName(), assignedTaskCount, concurTaskLimitPerWorker);
                    logger.info(strDetail);
                }
                return bRemove;
            });
        }

        nSize = listAgents.size();
        if (nSize == 0)
        {
            ret.resultCode = XFCommon.XFAgentSelectionResultCode.RESULT_NO_AVAILABLE_XFAGENT;
            String taskStr = taskToSend.toString();
            if (taskToSend.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_NO_AVAIL_AGENT)) {
                logger.warn("selectBestXFAgent can not find an available XFAgent server to execute this task: " + taskStr);
            } else {
                logger.debug("selectBestXFAgent can not find an available XFAgent server to execute this task: " + taskStr);
            }
            return ret;
        }

        XFAgent selectedXFAgent = null;
        if (nSize == 1)
        {
            // do nothing, no need to sort
            selectedXFAgent = listAgents.get(0);
            if (origSize > 1)
            {
                String strDetail = String.format("When routing taskid %s of priority %d, after initial screening, %s is the only server left, so it will be selected automatically.", taskId, taskPrio, selectedXFAgent.getServerName());
                logger.debug(strDetail);
            }
        }
        else
        {
            if (this.showselectbestworkerdetail)
            {
                String strOrderedAgents = "";
                for(XFAgent aXFAgent : listAgents)
                {
                    strOrderedAgents += " " + aXFAgent.getServerName();
                }
                String strDetail = String.format("When routing taskid %s of priority %d, before sorting, the ordered list of XFAgents is: %s ", taskId,taskPrio, strOrderedAgents);
                logger.info(strDetail);
            }
            XFAgentSelectionAlgorithm algor = new XFAgentSelectionAlgorithm(taskPrio, XFCommon.XFAgentSelectionAlgorithmType.ByLowerCPUAndHigherAvailablePhysicalMemoryInByte);
            listAgents.sort(algor);

            if (this.showselectbestworkerdetail)
            {
                String strOrderedAgents = "";
                for(XFAgent aXFAgent : listAgents)
                {
                    strOrderedAgents += " " + aXFAgent.getServerName();
                }
                String strDetail = String.format("When routing taskid %s of priority %d, after sorting, the ordered list of XFAgents is: %s ", taskId,taskPrio, strOrderedAgents);
                logger.info(strDetail);
            }

            // if many tasks happen at about the same time, we need to do round-robin between those XFAgents that are good enough
            Instant instNow = Instant.now();
            long secondsSinceLastTask = Duration.between(timestampLastTaskSelectXFAgent, instNow).toSeconds();
            if (secondsSinceLastTask < XFAGENT_TASK_ROUNDROBIN_INTERVAL_IN_SECONDS)
            {
                int minTaskCount = Integer.MAX_VALUE;
                for (XFAgent agentInMem : listAgents)
                {
                    String oneServerName = agentInMem.getServerName();
                    XFAgentMetadata selectedMetadata = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(oneServerName);
                    if (selectedMetadata != null)
                    {
                        int taskCount = selectedMetadata.getRecentTaskCount();
                        if (taskCount < minTaskCount)
                        {
                            minTaskCount = taskCount;
                            selectedXFAgent = agentInMem;

                            if (minTaskCount == 0)
                            {
                                break;
                            }
                        }
                    }
                }
            }
            else
            {
                selectedXFAgent = listAgents.get(0);
            }
        }

        // TODO: this block may be unreachable
        if (selectedXFAgent == null)
        {
            selectedXFAgent = listAgents.get(0);
            if (!taskToSend.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_SELECTED_AGENT)) {
                logger.warn("selectBestXFAgent had a problem, and the XFAgent {} will be used for this task {}", selectedXFAgent.getServerName(), taskToSend.ToLogString());
            } else {
                logger.debug("selectBestXFAgent had a problem, and the XFAgent {} will be used for this task {}", selectedXFAgent.getServerName(), taskToSend.ToLogString());
            }
        }

        ret.resultCode = XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED;

        String selectedServerName = selectedXFAgent.getServerName();
        String taskStr = taskToSend.toString();
        if (!taskToSend.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_SELECTED_AGENT)) {
            logger.info("selectBestXFAgent successfully selected the best XFAgent {} to execute this task: {} ", selectedServerName, taskToSend.getSelfTaskId());
        } else {
            logger.debug("selectBestXFAgent successfully selected the best XFAgent {} to execute this task: {} ", selectedServerName, taskToSend.getSelfTaskId());
        }
        ret.selectedQueueName  = String.format(XFCommon.XFAgent_task_queue_strformat, selectedServerName);
        if (workerQueueNameVersion == 1) {
            // Old version queue name, used by XFAgent built before Jan 23, 2018
            ret.selectedQueueName = selectedServerName + "_xfagent";
        }
        ret.selectedWorkerServerName = selectedServerName;

        this.timestampLastTaskSelectXFAgent = Instant.now();

        //simulateMemoryChangeAfterXFAgentSelectedForOneTask(selectedXFAgent, taskToSend);
        simulatePhysicalMemoryChangeAfterXFAgentSelectedForOneTask(selectedXFAgent, taskToSend);
        return ret;
    }

    private void simulatePhysicalMemoryChangeAfterXFAgentSelectedForOneTask(XFAgent xfAgentInMemory, XFTask taskToSend)
    {
        if (perTaskPhysicalMemMB <= 0 || ramHighWatermark >= 100) {
            // disable this check
            return;
        }

        long currentPhysicalAvail = xfAgentInMemory.getServerPhysicalAvailableMemoryInByte();
        long futurePhysicalAvail = currentPhysicalAvail - perTaskPhysicalMemMB * 1024L * 1024L;

        xfAgentInMemory.setServerPhysicalAvailableMemoryInByte(futurePhysicalAvail);

        if (xfAgentInMemory.getServerPhysicalTotalMemoryInByte() != 0 &&
                currentPhysicalAvail != 0) {
            if (ramHighWatermark <= 10 || ramHighWatermark > 100) {
                ramHighWatermark = 90;
            }

            if (futurePhysicalAvail > (xfAgentInMemory.getServerPhysicalTotalMemoryInByte() * (100 - ramHighWatermark) / 100)) {
                // there will be enough physcial memory
            } else {
                String strReason =
                        String.format("Worker server %s is under heavy load, put it in blacklist, total physical memory %d, total available physical memory will be %d",
                                xfAgentInMemory.getServerName(),
                                xfAgentInMemory.getServerPhysicalTotalMemoryInByte(),
                                futurePhysicalAvail);

                XFAgentMetadata metadata = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(xfAgentInMemory.getServerName());
                if(!metadata.isBlacklisted()) {
                    // print out a log line when switching from healthy to blacklisted
                    logger.warn("!!!!!!!!!! " + strReason);
                }
                metadata.setBlacklisted(true);
                metadata.setBlacklistedReason(strReason);
                metadata.setBlacklistedReasonCode(XFCommon.XFAgentBlacklistedReasonCode.REASON_DUE_TO_OVERLOAD);
            }
        }

        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(xfAgentInMemory);
    }

    /** flowengine need to adjust its in memory XFAgent data for a task that is going to be sent to this XFAgent,
     * so that for the next task if it is immediate after this task, we can avoid keep sending to the same XFAgent server.
     *
     * @param xfAgentInMemory
     * @param taskToSend
     */
    private void simulateMemoryChangeAfterXFAgentSelectedForOneTask(XFAgent xfAgentInMemory, XFTask taskToSend)
    {
        long p1BucketQuota = xfAgentInMemory.getP1BucketAllowedByte();
        long p1ActualUsed = xfAgentInMemory.getP1ActualUsedByte();
        long p2BucketQuota = xfAgentInMemory.getP2BucketAllowedByte();
        long p2ActualUsed = xfAgentInMemory.getP2ActualUsedByte();
        long p3BucketQuota = xfAgentInMemory.getP3BucketAllowedByte();
        long p3ActualUsed = xfAgentInMemory.getP3ActualUsedByte();

        int taskPrio = taskToSend.getTaskPriority();
        if (taskPrio == XFCommon.TASK_PRIORITY_SUPER)
        {
            p1ActualUsed += XFCommon.EstimatedXFTaskVirtualMemory;
            xfAgentInMemory.setP1ActualUsedByte(p1ActualUsed);
        }
        else if (taskPrio == XFCommon.TASK_PRIORITY_HIGH)
        {
            p2ActualUsed += XFCommon.EstimatedXFTaskVirtualMemory;
            xfAgentInMemory.setP2ActualUsedByte(p2ActualUsed);
        }
        else // treated as if (taskPrio == XFCommon.TASK_PRIORITY_LOW)
        {
            p3ActualUsed += XFCommon.EstimatedXFTaskVirtualMemory;
            xfAgentInMemory.setP3ActualUsedByte(p3ActualUsed);
        }

        long p1OverUsed = Math.max(0, p1ActualUsed - p1BucketQuota);
        long countedAsP2ActualUsed = p1OverUsed + p2ActualUsed;
        long p2OverUsed = Math.max(0, countedAsP2ActualUsed - p2BucketQuota);

        xfAgentInMemory.setP3AvailableByte(Math.max(0, p3BucketQuota - p2OverUsed - p3ActualUsed));
        xfAgentInMemory.setP3IsOverloaded(xfAgentInMemory.getP3AvailableByte() <= 0);

        long p2p3CombinedQuota = p2BucketQuota + p3BucketQuota;
        xfAgentInMemory.setP2AvailableByte( Math.max(0, p2p3CombinedQuota - countedAsP2ActualUsed - p3ActualUsed));
        xfAgentInMemory.setP2IsOverloaded(xfAgentInMemory.getP2AvailableByte() <= 0);

        // p1 available is all that are currently available
        long allActualUsedTotal = p1ActualUsed + p2ActualUsed + p3ActualUsed;
        long serverAlloweTotal = xfAgentInMemory.getServerVirtualMemoryTotalAllowedInByte();
        xfAgentInMemory.setP1AvailableByte(serverAlloweTotal - allActualUsedTotal);
        xfAgentInMemory.setP1IsOverloaded(xfAgentInMemory.getP1AvailableByte() <= 0);

        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(xfAgentInMemory);
    }

    /**
     * Persist an XFTask object, create a new XFTaskflow object and enqueue the XFTask
     *
     * @param task the task to run
     * @return the result of the submission
     */
    public SubmitTaskResult submitTask(XFTask task, AMQP.BasicProperties xfClientMsgProperties) {
        String taskFlowId = task.getRootTaskId();
        String selfTaskId = task.getId();
        XFTaskflow xfTaskflow = new XFTaskflow();
        xfTaskflow.setId(taskFlowId);
        xfTaskflow.setJobId(task.getJobId());

        task.setXfTaskflow(xfTaskflow);

        // NOTE: XFTask must be saved before XFTaskflow is saved, otherwise XFTaskflow might
        // think it is finished since there is no XFTasks associated with
        long pid = ProcessHandle.current().pid();
        String strTaskHistory = String.format("Task is saved in DB by floweninge of pid %d at %s", pid, Instant.now().toString());
        task.getTaskHistory().add(strTaskHistory);

        boolean bUpSertInMem = this.taskInMemoryRepository.upsertXFTask(selfTaskId, task);
        if (bUpSertInMem)
        {
            this.taskInMemoryRepository.syncFromMemoryToDB(selfTaskId);
        }
        else
        {
            logger.error("Failed to save XFTask selfTaskId=" + selfTaskId + " into memory.");
            // for root task, we want to be strict, i.e., if we can not save it, we failed the root task immediately
            return SubmitTaskResult.FAILED;
        }

        bUpSertInMem = this.taskflowInMemoryRepository.upsertXFTaskflow(taskFlowId, xfTaskflow);
        if (bUpSertInMem == false)
        {
            logger.error("Failed to save XFTaskflow taskFlowId=" + taskFlowId + " into memory.");
            // for root task, we want to be strict, i.e., if we can not save it, we failed the root task immediately
            return SubmitTaskResult.FAILED;
        }

        taskQueueManager.enqueue(new TaskRequest(task, xfClientMsgProperties));
        return SubmitTaskResult.OK;
    }

    /**
     * Persist an XFTask object and enqueue the task
     * This is usually called by Data Task Group trigger
     *
     * @param task the task to create
     * @return the result of creation
     */
    public SubmitTaskResult submitTriggeredTask(XFTask task) {

        long pid = ProcessHandle.current().pid();
        String strTaskHistory = String.format("Task is saved in DB by Task Engine on %s of pid %d at %s", commonUtil.localHostName,  pid, Instant.now().toString());
        task.getTaskHistory().add(strTaskHistory);
        String selfTaskId = task.getId();
        String dtgId = task.getDtgId();

        boolean bUpSertInMem = this.taskInMemoryRepository.upsertXFTask(selfTaskId, task);
        if (bUpSertInMem)
        {
            this.taskInMemoryRepository.syncFromMemoryToDB(selfTaskId);
        }
        else
        {
            logger.warn("Failed to save triggered XFTask selfTaskId=" + selfTaskId + ", dtgId=" + dtgId + " into memory.");
            return SubmitTaskResult.FAILED;
        }

        if(task.getTaskStatus() <= XFCommon.TASKSTATUS_Running){
            if(taskQueueManager.enqueue(new TaskRequest(task, null))){
                logger.debug("Triggered XFTask selfTaskId=" + selfTaskId + ", dtgId=" + dtgId + " has been enqueue the memory queue.");
            }
        }
        return SubmitTaskResult.OK;
    }

    /**
     * Persist a XFTask object(child task or next task), associate to a existing XFTaskflow object and enqueue the sub XFTask
     * @param subtask the subtask to run
     * @return the result of the submission
     */
    public SubmitTaskResult submitChildOrNextTask(XFTask subtask, AMQP.BasicProperties xfClientMsgProperties) {
        String taskFlowId = subtask.getTaskflowId();
        Optional<XFTaskflow> xfTaskflowOpt = taskflowInMemoryRepository.findById(taskFlowId, true, true);
        if (!xfTaskflowOpt.isPresent())
        {
            logger.error("submitChildOrNextTask failed because of failure to find its taskflow in memory for subtask " + subtask.ToLogString());
            return SubmitTaskResult.FAILED;
        }

        XFTaskflow xfTaskflow = xfTaskflowOpt.get();
        subtask.setXfTaskflow(xfTaskflow);

        // No need to save anything for taskflow, even if a new sub task is created for that task flow

        String selfTaskId = subtask.getId();
        boolean bUpSertInMem = this.taskInMemoryRepository.upsertXFTask(selfTaskId, subtask);
        if (bUpSertInMem == false)
        {
            logger.warn("Failed to upsert triggered XFTask selfTaskId=" + selfTaskId + " into memory and DB.");
            return SubmitTaskResult.FAILED;
        }

        taskQueueManager.enqueue(new TaskRequest(subtask, xfClientMsgProperties));
        return SubmitTaskResult.OK;
    }

    /**
     * Process a Task begin message. The status of this XFTask should already be updated by XFAgent
     * If this is the first task of a task flow, publish a message to the TaskCallbackQueue.
     * @return
     */
    public boolean processTaskBeginMessage(String selfTaskId, int xfagentProcessId, String xfagentServerName, Object passThroughProperties)
    {
        // TODO: Do not update status to Running if the task has completed or the taskflow has completed
        boolean ret = true;

        Optional<XFTask> optXFTask = getTask(selfTaskId);
        if (optXFTask.isPresent() == false) {
            logger.warn("Cannot find XFTask object for TaskEnd message received for selfTaskId " + selfTaskId);
            ret = false;
        } else {
            XFTask xftask = optXFTask.get();
            this.taskInMemoryRepository.updateXFAgentInfo(xftask, xfagentProcessId, xfagentServerName);
            if(xftask.getTaskStatus() < XFCommon.TASKSTATUS_Running){
                this.taskInMemoryRepository.updateStatus(xftask, XFCommon.TASKSTATUS_Running, "", null);
            }

            if (selfTaskId != null && selfTaskId.equals(xftask.getTaskflowId()))
            {
                // Notify XFClient when a seed task begins
                String taskCallBackQueue = xftask.getTaskCallbackQueue();
                // The Seed Task is the first XFTask whoes root task id is equal to the taskflow Id
                String taskflowId = xftask.getTaskflowId();
                String seedRootTaskId = taskflowId;
                Optional<XFTask> optSeedXFTask = getTask(seedRootTaskId);
                if (optSeedXFTask.isPresent() == false) {
                    logger.warn("Cannot find Seed XFTask object for TaskEnd message received for selfTaskId " + selfTaskId);
                    ret = false;
                } else {
                    String params = optSeedXFTask.get().getTaskParameters();
                    this.publisher.publishWithRetry(XFCommon.client_request_exchange, taskCallBackQueue, passThroughProperties, params);
                    logger.info("Notify taskFlow={} begin", xftask.getTaskflowId());
                    metrics.addTaskflowStartCount(1);
                }
            }
        }

        return ret;
    }

    /**
     * Process a Task end message. The status of this XFTask should already be updated by XFAgent
     * If the owning task flow completed, publish a message to the TaskCallbackQueue.
     * @return
     */
    public boolean processTaskEndMessage(String selfTaskId, int taskCompleteStatus)
    {
        return processTaskEndMessage(selfTaskId, taskCompleteStatus, null);
    }
    public boolean processTaskEndMessage(String selfTaskId, int taskCompleteStatus, String strTaskStatusFinalReason)
    {
        // TODO: Do not update status to Running if the task has completed or the taskflow has completed
        boolean ret = true;

        Optional<XFTask> optXFTask = getTask(selfTaskId);
        if (optXFTask.isPresent() == false) {
            logger.warn("Cannot find XFTask object for TaskEnd message received for selfTaskId " + selfTaskId);
            return false;
        }

        XFTask xftask = optXFTask.get();
        if(xftask.getTaskStatus() < taskCompleteStatus){
            this.taskInMemoryRepository.updateStatus(xftask, taskCompleteStatus, strTaskStatusFinalReason, null);
            if(taskCompleteStatus == XFCommon.TASKSTATUS_CompletedNormally){
                metrics.addTaskCompletedCount(1);
            }else if(taskCompleteStatus == XFCommon.TASKSTATUS_CompletedCrash){
                metrics.addTaskCrashedCount(1);
            }else if(taskCompleteStatus == XFCommon.TASKSTATUS_Canceled){
                metrics.addTaskCancelledCount(1);
            }
        }
        String taskflowId = xftask.getTaskflowId();

        Optional<XFTaskflow> optTaskFlow = taskflowInMemoryRepository.findById(taskflowId, true, true);
        if (optTaskFlow.isPresent() == false)
        {
            logger.warn("Cannot find XFTaskflow object for TaskEnd message received for selfTaskId " + selfTaskId + " whoes taskflowId = " + taskflowId);
            return false;
        }

        XFTaskflow xfTaskflow = optTaskFlow.get();
        boolean isAlreadyFinal = xfTaskflow.getStatusIsFinal();

        // update task flow status based on task's status
        CalculateTasklowStatus(taskflowId, true);

        // retrieve it again from DB
        optTaskFlow = taskflowInMemoryRepository.findById(taskflowId, true, true);
        xfTaskflow = optTaskFlow.get();
        if (!isAlreadyFinal && xfTaskflow.getStatusIsFinal())
        {
            return notifyTaskflowFinish(xfTaskflow);
        }

        return ret;
    }

    /**
     * notify XFClient the taskflow finish
     * @param xfTaskflow - finished taskflow
     * @return
     */
    public boolean notifyTaskflowFinish(XFTaskflow xfTaskflow){
        if(xfTaskflow == null || !xfTaskflow.getStatusIsFinal()){
            return false;
        }

        // The Seed Task is the first XFTask whose root task id is equal to the taskflow Id
        String seedRootTaskId = xfTaskflow.getId();
        Optional<XFTask> optSeedXFTask = getTask(seedRootTaskId);
        if (optSeedXFTask.isPresent() == false) {
            logger.warn("Cannot find Seed XFTask object for taskflow finish for taskflowId: " + seedRootTaskId);
            return false;
        }

        XFTask xfTask = optSeedXFTask.get();

        Map<String,Object> headers = new HashMap<>();
        headers.put(XFCommon.NBMSGVERSION, XFCommon.NBMSGVERSION_NB_IE_7_DOT_1);
        headers.put("user_name", xfTask.getUserName());
        headers.put("user_IPAddress", xfTask.getUserIP());
        headers.put("domainId", xfTask.getDomainId());
        headers.put("domainDbName", xfTask.getDomainDbName());
        headers.put("tenantId", xfTask.getTenantId());
        headers.put("tenantDbName", xfTask.getTenantDbName());
        headers.put("shortDescription", xfTask.getShortDescription());
        headers.put("WorkerRestartTimes", xfTask.getWorkerRestartTimes());
        headers.put("task_job_id", xfTask.getJobId());
        headers.put("parent_task_id", xfTask.getParentTaskId());
        headers.put("root_task_id", xfTask.getRootTaskId());
        headers.put("self_task_id", xfTask.getSelfTaskId());
        headers.put("taskflow_id", xfTask.getTaskflowId());
        //headers.put("task_node_id", );
        //headers.put("task_vehicle_id", );
        //headers.put("task_thread_id", );
        headers.put("task_type", xfTask.getTaskType());
        headers.put("jobRunCategory", xfTask.getJobRunCategory());
        headers.put("task_message_content_type", AMQPTaskGateway.TASK_TYPE_TASK_END);
        headers.put("worker_process_id", xfTask.getWorkerProcessId());
        headers.put("task_complete_status", xfTask.getTaskStatus());
        headers.put("task_run_error", xfTask.getErrorMessage());


        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .headers(headers)
                .replyTo("Nothing") // this has to be added, otherwise the C# code in RMClient string json = JsonConvert.SerializeObject(ea, TaskReqContext.instance.jsonSettings); will thrown exception
                .contentType("application/json")
                .deliveryMode(2).build();

        String taskCallBackQueue = xfTask.getTaskCallbackQueue();
        this.publisher.publishWithRetry(XFCommon.client_request_exchange,
                taskCallBackQueue, props, xfTask.getTaskParameters());
        logger.info("Notify taskFlow={} end", xfTaskflow.getId());
        metrics.addTaskflowEndCount(1);
        return true;
    }

    /**
     * Find XFTaskflow and stop it
     * @param strJobIdOrTaskflowId - the JobId or the TaskflowId
     * @param nStopTimeoutInSecond - the timeout value in seconds before XFAgent kills the worker
     * @return
     */
    public boolean stopTaskflowByJobIdOrTaskflowId(String strJobIdOrTaskflowId, boolean bIdIsJobId, int nStopTimeoutInSecond, String strReason )
    {
        List<XFTaskflow> xfTaskflows = null;
        if (bIdIsJobId)
        {
            xfTaskflows = taskflowInMemoryRepository.findRunningTaskflowByJobId(strJobIdOrTaskflowId);
        }
        else
        {
            xfTaskflows = taskflowInMemoryRepository.findRunningTaskflowByTaskflowId(strJobIdOrTaskflowId);
        }

        if(xfTaskflows == null || xfTaskflows.isEmpty()){
            return false;
        }

        // TODO: stop DTG first
        boolean bStopTaskOK = this.stopTaskByJobIdOrTaskflowId(xfTaskflows, strJobIdOrTaskflowId, bIdIsJobId, nStopTimeoutInSecond, strReason);
        boolean bStopDtgOK = this.stopDtgByJobIdOrTaskflowId(strJobIdOrTaskflowId, bIdIsJobId, strReason);

        Timer time = new Timer();
        Function<String, Integer> delayCheckAndNotify = (String taskFlowId)->{
            if(nStopTimeoutInSecond < 0){
                return 1;
            }

            // The nStopTimeoutInSecond value is used to instruct XFAgent to wait that long before killing a
            // workershell process, and thus we should wait a little longer (2 second) than that value.
            int nTimeoutPlusOverhead = nStopTimeoutInSecond + XFCommon.CANCELTASKFLOW_TIMEOUT_OVERHEAD_INSECONDS;
            time.schedule(new java.util.TimerTask(){
                @Override
                public void run(){
                    XFTaskflow xfTaskflow = taskflowInMemoryRepository.findTaskflowById(taskFlowId);
                    if(!xfTaskflow.getStatusIsFinal()){
                        logger.warn("Taskflow id {} is still not completed after the given timeout {} seconds, thus marking it as {} and all its tasks as {} ",  strJobIdOrTaskflowId, nStopTimeoutInSecond, XFCommon.TASKFLOWSTATUS_Canceled, XFCommon.TASKSTATUS_Canceled);
                        taskInMemoryRepository.stopXFTasksByTaskflowId(taskFlowId, strReason);
                        xfTaskflow.setStatus(XFCommon.TASKFLOWSTATUS_Canceled);
                        taskflowInMemoryRepository.upsertXFTaskflow(taskFlowId, xfTaskflow);
                        notifyTaskflowFinish(xfTaskflow);
                    }
                }
            }, nTimeoutPlusOverhead * 1000);
            return 0;
        };

        for(XFTaskflow xfTaskflow : xfTaskflows){
            String taskflowId = xfTaskflow.getId();
            this.CalculateTasklowStatus(xfTaskflow.getId(), true);

            xfTaskflow = taskflowInMemoryRepository.findTaskflowById(taskflowId);
            if(xfTaskflow.getStatusIsFinal()){
                //no task running or will run, but maybe notify twice
                this.notifyTaskflowFinish(xfTaskflow);
            }else{
                //delay check finish and force notify taskflow finish, but maybe notify twice
                delayCheckAndNotify.apply(taskflowId);
            }
        }

        return (bStopTaskOK && bStopDtgOK);
    }

    public boolean stopDtgByJobIdOrTaskflowId(String strJobIdOrTaskflowId, boolean bIdIsJobId, String strReason )
    {
        boolean bCancelFSCOK = true;
        // First, Notify FSC to cancel the triggering
        List<XFDtg> dtgList = null;
        if (bIdIsJobId)
        {
            dtgList = dtgRepository.findDtgsByJobId(strJobIdOrTaskflowId);
        }
        else
        {
            dtgList = dtgRepository.findDtgsByTaskflowId(strJobIdOrTaskflowId);
        }

        for (XFDtg dtg: dtgList) {
            if(dtg.getDtgStatus() >= XFCommon.TASKSTATUS_CompletedNormally){
                continue;
            }
            FrontServerController fsc = fscRepository.findFSCByTenantId(dtg.getTenantId());
            if (fsc != null) {
                NetlibClient netlibClient = new NetlibClient(fsc, metrics);
                try {
                    logger.info("Trying to stop DTG {} for task/job {}", dtg.getId(), strJobIdOrTaskflowId);
                    netlibClient.sendCommand(FSCRequest.CMD_fsStopDTGReq, dtg.getId());
                } catch (InterruptedException e) {
                    logger.warn("Failed to stop DTG for task/job" + strJobIdOrTaskflowId, e);
                }
            }
        }
        // Then, need to delete (or mark it as deleted for all XFDtgs in database)
        boolean bStopDTGOK = dtgRepository.stopDtgsByJobIdOrTaskflowId(strJobIdOrTaskflowId);
        return  (bCancelFSCOK && bStopDTGOK);
    }

    // For OnDemand task, the caller gives RMClient the jobId which will be used as root taskId. In this case, the caller
    // will call stop by the original JobId, or the root taskId which is returned as an C# out parameter in the RMClient.RunJob function
    // For Scheduled task, the jobid will be different than the root task Id, if the user want to cancel the scheduled-run benchmark
    // the caller still gives RMClient the jobID to cancel the running task.
    private boolean stopTaskByJobIdOrTaskflowId(List<XFTaskflow> taskflows, String strJobIdOrTaskflowId, boolean bIdIsJobId, int nTimeoutInSeconds, String strReason)
    {
        XFTaskUtil xfTaskUtil = new XFTaskUtil();
        // if any tasks haven't starting running, kill them in their baby cribs
        for (XFTaskflow taskflow: taskflows) {
            taskflow.setStopRequested(true);
            List<XFTask> tasks = taskInMemoryRepository.findByTaskflow(taskflow);
            for (XFTask task: tasks) {
                if (xfTaskUtil.isBeforeRunning(task)) {
                    taskInMemoryRepository.updateStatus(task, XFCommon.TASKSTATUS_Canceled,
                            "Task is cancelled before it starts by Task Engine", null);
                }
            }
        }

        // populate the command_cancel_task command details
        Map<String,Object> headers = new HashMap<>();
        headers.put(XFCommon.NBMSGVERSION, XFCommon.NBMSGVERSION_NB_IE_7_DOT_1);
        headers.put(XFCommon.cancel_task_timeout, (int)nTimeoutInSeconds);
        headers.put(XFCommon.cancel_task_reason, strReason);

        ArrayList<String> taskflowToCancelIDs = new ArrayList<>();
        // if it is jobId, we need to find current running taskflow anc cancel that.
        if (bIdIsJobId)
        {

            String strJobId = strJobIdOrTaskflowId;
            taskflowToCancelIDs = this.getUnfinishedTaskflowsForJobId(strJobId);
        }
        else
        {

            String strTaskId = strJobIdOrTaskflowId;
            taskflowToCancelIDs.add(strTaskId);
        }

        // Set the type to TaskId
        headers.put("IDToCancelType", XFCommon.IDType_TaskId);
        for (String aTaskflowId : taskflowToCancelIDs)
        {
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .type(XFCommon.command_cancel_task)
                    .headers(headers)
                    .contentType("application/json")
                    .deliveryMode(2).build();
            // broadcast the command_cancel_task command to all XFAgents
            this.publisher.publishWithRetry(XFCommon.RMAgent_exchange, XFCommon.RMAgent_command, props, aTaskflowId);
        }

//        AMQP.BasicProperties props = new AMQP.BasicProperties
//                .Builder()
//                .type(XFCommon.command_cancel_task)
//                .headers(headers)
//                .contentType("application/json")
//                .deliveryMode(2).build();
//        // broadcast the command_cancel_task command to all XFAgents
//        this.publisher.publishWithRetry(XFCommon.RMAgent_exchange, XFCommon.RMAgent_command, props, strJobIdOrTaskflowId);

        return true;
    }

    public boolean stopFollowedTask(String currTaskId, int nTimeoutInSeconds, String strReason)
    {
        // populate the command_cancel_task command details
        Map<String,Object> headers = new HashMap<>();
        headers.put(XFCommon.NBMSGVERSION, XFCommon.NBMSGVERSION_NB_IE_7_DOT_1);
        headers.put(XFCommon.cancel_task_timeout, (int)nTimeoutInSeconds);
        headers.put(XFCommon.cancel_task_reason, strReason);
        headers.put("IDToCancelType", XFCommon.IDType_TaskId);

        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .type(XFCommon.command_cancel_task)
                .headers(headers)
                .contentType("application/json")
                .deliveryMode(2).build();
        // broadcast the command_cancel_task command to all XFAgents
        this.publisher.publishWithRetry(XFCommon.RMAgent_exchange, XFCommon.RMAgent_command, props, currTaskId);

        return true;
    }
    /**
     *
     * @param taskflowId
     * @param dtgId
     * @param selfTask - the task being checked
     * @return boolean indicating if there is still processing going on for this dtgId
     */
    public boolean isDtgStillBeingProcessed(String taskflowId, String dtgId, XFTask selfTask) {
        boolean bHasRuuningDtgs = this.hasAnyRunningDescendantDtgsForDtgId(taskflowId, dtgId);
        boolean bHasRunningTasks = this.hasOlderUnfinishedTasksForDtgId(taskflowId, dtgId, selfTask);

        return bHasRuuningDtgs == true || bHasRunningTasks == true;
    }

    public boolean isDtgCreatorStillBeingProcessed(String dtgId){
        Optional<XFDtg> xfdtgOpt = dtgRepository.findById(dtgId);
        if(!xfdtgOpt.isPresent()){
            return false;
        }

        XFDtg xfdtg = xfdtgOpt.get();
        Optional<XFTask> xftaskOpt = taskInMemoryRepository.findById(xfdtg.getRegisteredByTaskId(), true, false);
        if(!xftaskOpt.isPresent()){
            return false;
        }

        return !xftaskOpt.get().getStatusIsFinal();
    }

    private boolean hasAnyRunningDescendantDtgsForDtgId(String taskflowId, String dtgId) {
        List<XFDtg> foundXFDtgList = dtgRepository.findRunningDescendantDtgsForDtgId(taskflowId, dtgId);
        if (foundXFDtgList != null && foundXFDtgList.size() >0 )
        {
            return true;
        }
        return false;
    }

    private boolean hasAnyRunningDtgsForDtgId(String dtgId) {
        List<XFDtg> foundXFDtgList = dtgRepository.findRunningDtgsForDtgId(dtgId);
        if (foundXFDtgList != null && foundXFDtgList.size() >0 )
        {
            return true;
        }
        return false;
    }

    private boolean hasAnyRunningTasksForDtgId(String dtgId) {
        List<XFTask> foundList = taskInMemoryRepository.findRunningTasksForDtgId(dtgId);
        if (foundList != null )
        {
            if (foundList.size() > 0)
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasOlderUnfinishedTasksForDtgId(String taskflowId, String dtgId, XFTask selfTask) {
        List<XFTask> foundList = taskInMemoryRepository.findOlderUnfinishedTasksForDtgId(taskflowId, dtgId, selfTask);
        if (foundList != null )
        {
            if (foundList.size() > 0)
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyUnfinishedTaskflowsForJobId(String jobId) {
        boolean bExists = false;
        Collection unfinishedStates = (new XFTaskflow()).getUnfinishedStates();
        List<XFTaskflow> runningTaskflows = taskflowInMemoryRepository.findByJobIdAndStatusIn(jobId, unfinishedStates);
        bExists = isExists(jobId, bExists, unfinishedStates, runningTaskflows);
        if(!bExists) {
           runningTaskflows= taskflowRepository.findRunningTaskflowByJobIdOrTaskflowId(jobId);
           bExists = isExists(jobId, bExists, unfinishedStates, runningTaskflows);
        }
        return bExists;
    }

    private boolean isExists(String jobId, boolean bExists, Collection unfinishedStates,
            List<XFTaskflow> runningTaskflows) {
        for (XFTaskflow aXFTaskflow: runningTaskflows) {
            if (aXFTaskflow.getJobId().equals(jobId))
            {
                String taskflowId = aXFTaskflow.getId();
                CalculateTasklowStatus(taskflowId, true);
                if (unfinishedStates.contains(aXFTaskflow.getStatus())) {
                    bExists = true;
                    break;
                }
            }
        }
        return bExists;
    }

    public ArrayList<String> getUnfinishedTaskflowsForJobId(String jobId) {
        ArrayList<String> results = new ArrayList<String>();
        if (StringUtil.isNullOrEmpty(jobId))
        {
            return results;
        }

        Collection unfinishedStates = (new XFTaskflow()).getUnfinishedStates();
        List<XFTaskflow> runningTaskflows = taskflowInMemoryRepository.findByJobIdAndStatusIn(jobId, unfinishedStates);
        for (XFTaskflow aXFTaskflow: runningTaskflows) {
            if (aXFTaskflow.getJobId().equals(jobId))
            {
                String taskflowId = aXFTaskflow.getId();
                CalculateTasklowStatus(taskflowId, true);
                if (unfinishedStates.contains(aXFTaskflow.getStatus())) {
                    results.add(taskflowId);
                    break;
                }
            }
        }
        return results;
    }

    public int CalculateTasklowStatus(String taskflowId){
        return CalculateTasklowStatus(taskflowId, false);
    }
    /**
     * Calculate status of a taskFlow.
     * The logic is as follows:
     * 1. If the 'status" filed in the XFTaskFlow document in DB is already final, return that status
     * 2. Otherwise, need to
     * * * 2.1 - query XFDtg collection for any document of that taskflowId with isFinalTriggerReceived==false
     * * * 2.2 - query XFTask collection for any root tasks of that taskflowId that is still not in final status
     * 3. Update the XFTaskFlow's status based on step #2 query results
     * @param taskflowId
     * @forceCalc force calculate
     * @return  the newly calcuated status
     */

    public int CalculateTasklowStatus(String taskflowId, boolean forceCalc)
    {
        Optional<XFTaskflow> optTaskFlow = taskflowInMemoryRepository.findById(taskflowId, true, true);
        if (optTaskFlow.isPresent() == false)
        {
            logger.warn("Can not find XFTaskFlow for taskflowId %s", taskflowId);
            return XFTaskflow.STATUS_NOT_EXIST;
        }

        XFTaskflow xftaskflow = optTaskFlow.get();
        if (xftaskflow.getStatusIsFinal())
        {
            return xftaskflow.getStatus();
        }

        if(!forceCalc){
            long queryTimeGap = ChronoUnit.SECONDS.between(xftaskflow.getStatusUpdatedTime(), Instant.now());
            if (xftaskflow.getStatusPotentialDirty() == false && queryTimeGap < this.FLOWSTATUS_FORCE_REFRESH_TIME_INTERVAL)
            {
                return xftaskflow.getStatus();
            }
        }
        
        int calcStatus = XFTaskflow.STATUS_Scheduled;
        boolean bHasRunningTasks = false;
        boolean bHasRunningDtgs = false;
        bHasRunningDtgs = this.hasAnyRunningDtgsForTaskflowId(taskflowId);
        if (bHasRunningDtgs)
        {
            calcStatus = XFTaskflow.STATUS_Running;
        }
        else {
            // TODO: we only check root task status, consider checking all tasks?
            bHasRunningTasks = this.hasAnyRunningRootTasksForTaskflowId(taskflowId);
            if (bHasRunningTasks)
            {
                calcStatus = XFTaskflow.STATUS_Running;
            }
        }

        if (bHasRunningTasks == false && bHasRunningDtgs == false)
        {
            calcStatus = XFTaskflow.STATUS_CompletedNormally;
        }

        // save it back to db when the newly calculated status is different with the current status
        if (xftaskflow.getStatus() < calcStatus)
        {
            xftaskflow.setStatus(calcStatus);
            taskflowInMemoryRepository.upsertXFTaskflow(taskflowId, xftaskflow);
        }
        
        return xftaskflow.getStatus();
    }

    private boolean hasAnyRunningDtgsForTaskflowId(String taskflowId)
    {
        List<XFDtg> foundXFDtgList = dtgRepository.findRunningDtgsForTaskflowId(taskflowId);
        if (foundXFDtgList != null && foundXFDtgList.size() > 0)
        {
            return true;
        }
        return false;
    }

    private boolean hasAnyRunningRootTasksForTaskflowId(String taskflowId)
    {
        List<XFTask> foundList = taskInMemoryRepository.findRunningRootTasksForTaskflowId(taskflowId);
        if (foundList != null && foundList.size() > 0)
        {
            return true;
        }

        for (int tryTimes=0; tryTimes < 10; tryTimes++)
        {
            try
            {
                foundList = taskRepository.findRunningRootTasksForTaskflowId(taskflowId);
                if (foundList != null && foundList.size() > 0)
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
            catch (Exception ex)
            {
                logger.warn("Exception happened taskRepository.findRunningRootTasksForTaskflowId " + taskflowId + " tryTimes=" + tryTimes , ex);
            }

            int sleepTime = 5 * 1000;
            try {

                Thread.sleep(sleepTime);
            } catch (Exception se) {
                logger.warn("Exception happened while sleeping " + sleepTime + " during hasAnyRunningRootTasksForTaskflowId " , se);
            }
        }

        logger.warn("!!!!!!!!!Exception happened in hasAnyRunningRootTasksForTaskflowId( " + taskflowId + "), we would rather assume that there might be tasks for this taskflow, so return true here" );
        return false;
    }
}
