package com.netbrain.xf.flowengine.queue;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import com.netbrain.xf.flowengine.config.FlowEngineConfig;
import com.netbrain.xf.flowengine.dao.XFAgentRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskInMemoryRepository;
import com.netbrain.xf.flowengine.metric.Metrics;
import com.netbrain.xf.flowengine.utility.CommonUtil;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.utility.HASupport;
import com.netbrain.xf.flowengine.workerservermanagement.UnackedXFTaskInfo;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentHeartBeatMessage;
import com.netbrain.xf.flowengine.daoinmemory.XFAgentInMemoryRepository;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentMetadata;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.xfcommon.XFCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.model.XFTask;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.netty.util.internal.StringUtil;

@Service
public class TaskQueueManagerImpl implements ITaskQueueManager {


    private static final int Accept_Ok = 0;
    private static final int Accept_Canceled = 1;
    private static final int Accept_DuplicateTask = 2;
    private static final int Accept_Crashed = 3;
    private static final int Accept_UnknownContentType = 4;
    private static final int Accept_Exception = 5;
    private static final int Accept_NoQueueExist = 6;
    private static final int Accept_Failed = 7;
    //below need to redispatch
    private static final int Retry_InvalidPriority = 8;
    private static final int Retry_Priority = 9;
    private static final int Retry_NoResource = 10;

    private static final int NEED_REDISPATCH = Accept_Failed;

    private  Map<String,String> xfagentMap = new ConcurrentHashMap<String,String>();
    private static Logger logger = LogManager.getLogger(TaskQueueManagerImpl.class.getSimpleName());

    @Value("${workerserver.queue.name.version}")
    private int workerQueueNameVersion;

    @Value("${workerserver.servernames}")
    private String workerserverNames;

    @Value("${taskqueuemanager.dequeuer.enabled}")
    private boolean dequeuerEnabled;

    @Value("${taskqueuemanager.dequeuer.send.delay.ms}")
    private int sendDelayMs;

    @Value("${workerserver.selection.exclude.overloaded.worker}")
    private boolean excludeOverloadedWorker;

    @Value("${background.xftaskmaintenance.task_unack_timelimit_in_seconds}")
    private int task_unack_timelimit_in_seconds;

    @Autowired
    AMQPClient amqpClient;

    @Autowired
    private TaskController taskController;
    
    @Autowired
    private XFTaskInMemoryRepository taskInMemoryRepository;

    @Autowired
    private XFAgentRepository xfAgentRepository;

    @Autowired
    XFAgentInMemoryRepository xfAgentInMemoryRepository;

    @Autowired
    private FlowEngineConfig flowEngineConfig;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private HASupport haSupport;

    @Autowired
    private DataCenterSwitching dcSwitching;

    @Autowired
    private Metrics metrics;

    private Channel channel;

    private PriorityBlockingQueue<TaskRequest> taskQueue = new PriorityBlockingQueue<>();
    private LinkedBlockingQueue<TaskRequest> delayTaskQueue = new LinkedBlockingQueue<>();

    protected PriorityBlockingQueue<TaskRequest> getTaskQueue() {
        return taskQueue;
    }

    public TaskRequest[] exportQueueElements() {
        return taskQueue.toArray(new TaskRequest[0]);
    }

    public boolean enqueue(TaskRequest taskRequest) {
        XFTask xfTask = taskRequest.getXfTask();
        if(xfTask != null){
            // TODO: only check less than scheduled
            if(xfTask.getTaskStatus() <= XFCommon.TASKSTATUS_Scheduled){
                taskInMemoryRepository.updateStatus(xfTask, XFCommon.TASKSTATUS_Scheduled, "", null);
            }

            String strLogMsg = String.format("TaskQueueManager is enqueuing the task %s whose taskStatus is %d . ", xfTask.getId(), xfTask.getTaskStatus());
            logger.debug(strLogMsg);
            taskQueue.add(taskRequest);
        }else{
            return false;
        }

    	return true;
    }

    /**
     * Keep a long running thread to dequeue tasks from taskQueue and send
     * those tasks to XFAgent to execute.
     */
    @PostConstruct
    public void dequeueAndSendToXFAgent() {
        if (dequeuerEnabled) {
            // dequeue is only disabled in unit testing environment
            Connection mqConnection = amqpClient.getMqConnection();
            try {
                channel = mqConnection.createChannel();
            } catch (IOException e) {
                logger.error("Failed to create RMQ channel", e);
            }

            rpcHandleRequests();
            Timer time = new Timer();
            Function<TaskRequest, Integer> delayEnqueue = (TaskRequest taskRequest)->{
                time.schedule(new java.util.TimerTask(){
                    @Override
                    public void run(){
                        enqueue(taskRequest);
                    }
                }, 1000);
                return 0;
            };

            metrics.setTaskPendingSnapshotCallback((Long unuse)->{
                return (long)taskQueue.size();
            });

            Runnable dequeuer = new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (!haSupport.isActive() || !dcSwitching.isActiveDC()) {
                            logger.debug("Noop in standby mode or inactive DC.");
                            break;
                        }
                        TaskRequest taskRequest = null;
                        try {
                            taskRequest = taskQueue.take();
                            if (taskRequest == null) {
                                continue;
                            }

                            XFTask xfTask = taskRequest.getXfTask();
                            if(xfTask == null){
                                continue;
                            }

                            String taskidTosend = xfTask.getSelfTaskId();

                            String dtgId = xfTask.getDtgId();
                            boolean bTaskHasDtg = false;
                            if (dtgId != null && !dtgId.isEmpty())
                            {
                                bTaskHasDtg = true;
                            }

                            Optional<XFTask> taskInMemOpt = taskInMemoryRepository.findById(taskidTosend);
                            if (taskInMemOpt.isPresent() )
                            {
                                XFTask task = taskInMemOpt.get();
                                int theTaskStatus = task.getTaskStatus();
                                if (theTaskStatus >= XFCommon.TASKSTATUS_Running)
                                {
                                    // if task is already running (which will probably be blocked by XFAgent's redis duplicate taskid check
                                    // or task is already finished (which Redis key have been deleted),
                                    // flowengine should not send this task to xfagent
                                    logger.debug("Will NOT send task={} to anywhere because taskStatus is already {} in DB", taskidTosend, theTaskStatus);
                                    continue;
                                }
                            }
                            else
                            {
                                logger.warn("Warning sign, failed to find task {} in memory and DB, but flowengine will continue to send this task to worker server anyway. ", taskidTosend);
                            }
                            
                            if (bTaskHasDtg == false
                                    || (!taskController.isDtgStillBeingProcessed(xfTask.getTaskflowId(), dtgId, xfTask)
                                        && !taskController.isDtgCreatorStillBeingProcessed(dtgId))){
                                int retCode = sendTaskToAgent(taskRequest);
                                if (retCode == XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED)
                                {
                                    // nothing need to be done
                                } else if (retCode == XFCommon.XFAgentSelectionResultCode.RESULT_NO_XFAGENT) {
                                    logger.warn("sendTaskToAgent failed because of no available XFAgent, this task will be put at the tail of the queue , task {}", xfTask.toString());
                                    delayEnqueue.apply(taskRequest);

                                } else if (retCode == XFCommon.XFAgentSelectionResultCode.RESULT_NO_AVAILABLE_XFAGENT) {
                                    if (!xfTask.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_NO_AVAIL_AGENT)) {
                                        logger.warn("sendTaskToAgent failed to find an available XFAgent server, this task will be put at the tail of the queue , task {}", xfTask.toString());
                                    } else {
                                        logger.debug("sendTaskToAgent failed to find an available XFAgent server, this task will be put at the tail of the queue , task {}", xfTask.toString());
                                    }
                                    delayEnqueue.apply(taskRequest);
                                } else if (retCode == XFCommon.XFAgentSelectionResultCode.SENDRESULT_XFAGENT_TASK_EXPIRED) {
                                    logger.warn("sendTaskToAgent failed because task TTL expired, task {} will be marked as {} ", xfTask.getSelfTaskId(), XFCommon.TASKSTATUS_Canceled);
                                    String strReason =
                                            String.format("Task TTL expired, task %s will be marked as %d on TaskEngine %s at time %s",
                                                    xfTask.getSelfTaskId(), XFCommon.TASKSTATUS_Canceled, commonUtil.localHostName, Instant.now().toString());
                                    taskController.processTaskEndMessage(xfTask.getSelfTaskId(), XFCommon.TASKSTATUS_Canceled, strReason);
                                } else {
                                    logger.warn("sendTaskToAgent failed because of unknown error code {}, this task will be discarded , task {}", retCode, xfTask.toString());
                                }
                            } else if(xfTask.isFinalTrigger()) {
                                //skip but need to set status
                                String strForceToExecuteReason =
                                        String.format("DTG triggered task(selfTaskId=%s, dtgId=%s) is the final trigger and thus will NOT be skipped even though the previous task has not finished yet. Just need to wait for the previous triggered task to finish.",
                                                xfTask.getId(), xfTask.getDtgId());
                                if (!xfTask.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_REQUEUE_FINAL_TRIGGERED)) {
                                    logger.info("sendTaskToAgent failed because: {} ", strForceToExecuteReason);
                                } else {
                                    logger.debug("sendTaskToAgent failed because: {} ", strForceToExecuteReason);
                                }
                                // xftothink, is this the best place to apply the logic of preventing triggered task to be run if the previous tasks for this same dtg is still unfinished?
                                delayEnqueue.apply(taskRequest);
                            } else {
                                //skip but need to set status
                                // This is almost unreachable since tasks are skipped in TriggerReceiver
                                String strSkipReason =
                                        String.format("DTG triggered task(selfTaskId=%s, dtgId=%s) will be skipped because the previous task has not finished yet",
                                                xfTask.getId(), xfTask.getDtgId());
                                logger.info("sendTaskToAgent failed because: {} ", strSkipReason);
                                taskInMemoryRepository.updateStatus(xfTask, XFCommon.TASKSTATUS_MergeAndSkip, strSkipReason, null);
                            }

                        } catch (Exception e) {
                            logger.error("Failed to dequeue a task ", e);
                            if(taskRequest != null){
                                delayEnqueue.apply(taskRequest);
                            }
                        }
                    }
                }
            };
            ExecutorService executor = Executors.newFixedThreadPool(1);
            executor.submit(dequeuer);
        }
    }

    // avoid concurrency when consuming and acking messages, see https://www.rabbitmq.com/api-guide.html
    private synchronized void publishMessage(String requestQueueName, AMQP.BasicProperties props, byte[] body) throws IOException {
        channel.basicPublish("", requestQueueName, props, body);
    }

    private void sendMessageWithDelay(Channel channel, String requestQueueName, AMQP.BasicProperties props, byte[] body) throws IOException {
        publishMessage(requestQueueName, props, body);
        if (sendDelayMs > 0) {
            try {
                Thread.sleep(sendDelayMs);
            } catch (InterruptedException e) {
                logger.error("Exception happened when delaying sending request");
            }
        }
    }

    /**
     * 
     * @param taskRequest
     * @throws IOException
     * @throws InterruptedException
     */
    private int sendTaskToAgent(TaskRequest taskRequest) throws IOException, InterruptedException
    {
        XFTask taskToSent = taskRequest.getXfTask();
        String taskidTosend = taskRequest.getXfTask().getId();
        Instant taskExpireTime = taskToSent.getExpireTime();
        long remainingMessageTTLInMS = -1;

        String remainingMessageTTLInMSStr = null;
        if (taskExpireTime != null ) {
            Instant instNow = Instant.now();
            if (taskExpireTime.isBefore(instNow) || taskExpireTime.equals(instNow)) {
                // already expired, and this task should be discarded
                logger.info("Will NOT send task={} to anywhere because task is already expired ", taskidTosend);
                return XFCommon.XFAgentSelectionResultCode.SENDRESULT_XFAGENT_TASK_EXPIRED;
            } else {
                // even if it is not expired yet, we should set the RabbitMQ per-message TTL so that it might expire in RabbitMQ queue to XFAgent
                remainingMessageTTLInMS = Duration.between(instNow, taskExpireTime).toMillis();
                remainingMessageTTLInMSStr = Long.toString(remainingMessageTTLInMS);
            }
        }

        int nWaitTimeWhenBusy = 1;

        XFCommon.XFAgentSelectionResult selRes = taskController.selectBestXFAgent(taskToSent, true);
        if (selRes.resultCode == XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED) {
            // update task's workerMachineName
            // taskInMemoryRepository.updateWorkerServerName(taskToSent, selRes.selectedWorkerServerName);
        } else if (selRes.resultCode == XFCommon.XFAgentSelectionResultCode.RESULT_NO_XFAGENT) {
            logger.warn("Failed to find best XFAgent for task {}, please check your configuration file and make sure the worker server(s) are running correctly!", taskidTosend);
            return selRes.resultCode;
        } else if (selRes.resultCode == XFCommon.XFAgentSelectionResultCode.RESULT_NO_AVAILABLE_XFAGENT) {
            if (!taskToSent.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_NO_AVAIL_AGENT)) {
                logger.warn("0000000000 No available XFAgent server, task taskid={} will retry after a while, {} second(s).", taskidTosend, nWaitTimeWhenBusy);
            } else {
                logger.debug("0000000000 No available XFAgent server, task taskid={} will retry after a while, {} second(s).", taskidTosend, nWaitTimeWhenBusy);
            }
            Thread.sleep(1000 * nWaitTimeWhenBusy);

            // if excludeOverloadedWorker is false
            // for the second time, we can try sending a task to an overloaded worker so that we can get feedback even if it may get rejected
            selRes = taskController.selectBestXFAgent(taskToSent, excludeOverloadedWorker);
            if (selRes.resultCode == XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED) {
                // update task's workerMachineName
                // taskInMemoryRepository.updateWorkerServerName(taskToSent, selRes.selectedWorkerServerName);
            } else if (selRes.resultCode == XFCommon.XFAgentSelectionResultCode.RESULT_NO_XFAGENT) {
                logger.warn("Failed to find best XFAgent for task {} , please check your configuration file and make sure the worker server(s) are running correctly!", taskidTosend);
                return selRes.resultCode;
            } else if (selRes.resultCode == XFCommon.XFAgentSelectionResultCode.RESULT_NO_AVAILABLE_XFAGENT) {
                if (!taskToSent.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_NO_AVAIL_AGENT)) {
                    logger.warn("No available XFAgent server for too long, task taskid={} will not be re-tried this time. Requeue it.", taskidTosend);
                } else {
                    logger.debug("No available XFAgent server for too long, task taskid={} will not be re-tried this time. Requeue it.", taskidTosend);
                }
                return selRes.resultCode;
            } else {
                if (!taskToSent.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_DEQUEUE_UNKNOWN)) {
                    logger.warn("Failed to select best XFAgent for unknown reason, task taskid={} will not be executed!", taskidTosend);
                } else {
                    logger.debug("Failed to select best XFAgent for unknown reason, task taskid={} will not be executed!", taskidTosend);
                }
                return selRes.resultCode;
            }
        } else {
            if (!taskToSent.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_DEQUEUE_UNKNOWN)) {
                logger.warn("Failed to select best XFAgent for unknown reason, task taskid={} will not be executed!", taskidTosend);
            } else {
                logger.debug("Failed to select best XFAgent for unknown reason, task taskid={} will not be executed!", taskidTosend);
            }
            return selRes.resultCode;
        }

        String requestQueueName = selRes.selectedQueueName;

        XFTask task = taskRequest.getXfTask();

        Map<String,Object> headers = new HashMap<>();
        if (taskRequest.getAmqpMsgProperties() != null) {
            headers = taskRequest.getAmqpMsgProperties().getHeaders();
        } else {
            headers.put(XFCommon.NBMSGVERSION, XFCommon.NBMSGVERSION_NB_IE_7_DOT_1);
            headers.put("task_id", task.getId());
            headers.put("task_job_id", task.getJobId());
            headers.put("shortDescription", task.getShortDescription());
            headers.put("jobRunCategory", task.getJobRunCategory());
            headers.put("task_type", task.getTaskType());
            headers.put("task_priority", task.getTaskPriority());
            headers.put("user_name", task.getUserName());
            headers.put("user_IPAddress", task.getUserIP());
            headers.put("tenantId", task.getTenantId());
            headers.put("tenantDbName", task.getTenantDbName());
            headers.put("domainId", task.getDomainId());
            headers.put("domainDbName", task.getDomainDbName());
            headers.put("WorkerRestartTimes", task.getWorkerRestartTimes());
            headers.put("task_callback_queue", task.getTaskCallbackQueue());
            headers.put("needBroadCallbackToAllApiServer", task.isNeedBroadCallbackToAllApiServer());

            if(StringUtil.isNullOrEmpty(task.getParentTaskId())){
                headers.put("task_message_content_type", "task_message_content_type_task");
            }
            else{
                headers.put("task_message_content_type", "task_message_content_type_sub_task");
                headers.put("parent_task_id", task.getParentTaskId());
            }

            headers.put("self_task_id", task.getId());
            headers.put("root_task_id", task.getRootTaskId());
            headers.put("ancestor_task_ids", task.getMaterializedPathToParent());
        }
        headers.put("taskflow_id", task.getTaskflowId());
        headers.put("finalTrigger", task.isFinalTrigger());

        int taskRabbitmqPriority = task.getTaskRabbitmqPriority();

        // if the there is already an application layer provided TTL, we use that ttl
        // otherwise, TaskEngine explicitly add a TTL that is equal to task_unack_timelimit_in_seconds
        AMQP.BasicProperties props;
        if (StringUtils.isEmpty(remainingMessageTTLInMSStr)) {
            props = new AMQP.BasicProperties
                    .Builder()
                    .priority(taskRabbitmqPriority)
                    .expiration(Long.toString(task_unack_timelimit_in_seconds))
                    .headers(headers)
                    .replyTo(xfagentMap.get(requestQueueName))
                    .contentType("application/json")
                    .deliveryMode(2).build();
        } else {
            props = new AMQP.BasicProperties
                    .Builder()
                    .priority(taskRabbitmqPriority)
                    .expiration(remainingMessageTTLInMSStr)
                    .headers(headers)
                    .replyTo(xfagentMap.get(requestQueueName))
                    .contentType("application/json")
                    .deliveryMode(2).build();
        }

        XFAgentMetadata agentMetadata = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(selRes.selectedWorkerServerName);
        int resentTimes = 0;
        if (agentMetadata != null) {
            Instant timeNow = Instant.now();

            UnackedXFTaskInfo unackedXFTaskInfo = agentMetadata.getOneUnackedXFTask(taskidTosend);
            if (unackedXFTaskInfo != null) {
                resentTimes = unackedXFTaskInfo.getResendTimes() + 1;
            }
            unackedXFTaskInfo = new UnackedXFTaskInfo(task, timeNow, resentTimes);
            agentMetadata.addOrUpdateOneUnackedXFTask(unackedXFTaskInfo);

            this.xfAgentInMemoryRepository.addOrUpdateOneUnackedXFTaskInfo(selRes.selectedWorkerServerName, unackedXFTaskInfo);
        }

        if (resentTimes == 0) {
            //xfQ&A, too early to set the taskStatus to Started?
            taskInMemoryRepository.updateStatus(task, XFCommon.TASKSTATUS_Started, "", Instant.now());
            sendMessageWithDelay(channel, requestQueueName, props, task.getTaskParameters().getBytes("UTF-8"));
            logger.debug("Sending task={} to queue={} for the first time", taskidTosend, requestQueueName );

            XFAgentMetadata selectedMetadata = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(selRes.selectedWorkerServerName);
            if (selectedMetadata != null)
            {
                selectedMetadata.setRecentTaskCount(selectedMetadata.getRecentTaskCount() + 1);
            }
        } else {
            Optional<XFTask> taskOpt = this.taskInMemoryRepository.findById(taskidTosend);
            if (taskOpt.isPresent() == false) {
                logger.warn("Will NOT resend task={} to queue={} for the {} time(s)  because of failure to retrieve the task", taskidTosend, requestQueueName, resentTimes);
            } else {
                XFTask xftask = taskOpt.get();
                int theTaskStatus = xftask.getTaskStatus();
                if (theTaskStatus >= XFCommon.TASKSTATUS_Running) {
                    // if task is already running (which will probably be blocked by XFAgent's redis duplicate taskid check
                    // or task is already finished (which Redis key have been deleted),
                    // flowengine should not send this task to xfagent
                    if (!xftask.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_DEQUEUE_ALREADY_DONE)) {
                        logger.warn("Will NOT resend task={} to queue={} for the {} time(s)  because taskStatus  is already in {} ", taskidTosend, requestQueueName, resentTimes, theTaskStatus);
                    } else {
                        logger.debug("Will NOT resend task={} to queue={} for the {} time(s)  because taskStatus  is already in {} ", taskidTosend, requestQueueName, resentTimes, theTaskStatus);
                    }
                } else {
                    sendMessageWithDelay(channel, requestQueueName, props, task.getTaskParameters().getBytes("UTF-8"));
                    if (!xftask.checkAndMarkLogged(XFTask.XFTASK_ACTIONLOG_REQUEUE)) {
                        logger.warn("Resending task={} to queue={} for the {} time(s)", taskidTosend, requestQueueName, resentTimes);
                    } else {
                        logger.debug("Resending task={} to queue={} for the {} time(s)", taskidTosend, requestQueueName, resentTimes);
                    }
                    XFAgentMetadata selectedMetadata = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(selRes.selectedWorkerServerName);
                    if (selectedMetadata != null) {
                        selectedMetadata.setRecentTaskCount(selectedMetadata.getRecentTaskCount() + 1);
                    }
                }
            }
        }

        return XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED;
    }

    public void addWorkerservers (String[] workerservernameArray) {
        for (String origAgentHostname : workerservernameArray) {
            String agentHostname = StringUtils.trimAllWhitespace(origAgentHostname);
            Optional<XFAgent> xfAgentOpt = this.xfAgentRepository.findById(agentHostname);
            if (xfAgentOpt.isPresent())
            {
                // usually this happens in a non-leader FlowEngine
                XFAgent xfagentmodelFromDB = xfAgentOpt.get();

                XFAgentMetadata agentMetadata = new XFAgentMetadata();
                agentMetadata.setServerName(agentHostname);
                agentMetadata.setUniqIdForEachUpdate(xfagentmodelFromDB.getUniqIdForEachUpdate());
                agentMetadata.setFirsttimeReceivedThisUniqId(Instant.now());
                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(xfagentmodelFromDB);
                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(agentMetadata);
                // do nothing
                logger.info("Find existing XFAgent record for serverName {}", agentHostname);
            }
            else
            {
                XFAgent xfagentmodelInMemory = new XFAgent();
                xfagentmodelInMemory.setId(agentHostname); // note, use the hostname as id
                String updateId = UUID.randomUUID().toString();
                xfagentmodelInMemory.setUniqIdForEachUpdate(updateId);
                xfagentmodelInMemory.setServerName(agentHostname);

                XFAgentMetadata agentMetadata = new XFAgentMetadata();
                agentMetadata.setServerName(agentHostname);
                agentMetadata.setUniqIdForEachUpdate(updateId);
                agentMetadata.setFirsttimeReceivedThisUniqId(Instant.now());

                logger.info("Adding new XFAgent record for serverName {}",agentHostname);
                this.xfAgentRepository.save(xfagentmodelInMemory);
                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(xfagentmodelInMemory);
                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(agentMetadata);
            }
        }

        String replyToQueueName = getReplyQueueName();
        for (String origAgentHostname : workerservernameArray) {
            String agentHostname = StringUtils.trimAllWhitespace(origAgentHostname);
            String requestQueueName = String.format(XFCommon.XFAgent_task_queue_strformat, agentHostname);
            if (workerQueueNameVersion == 1) {
                requestQueueName = agentHostname + "_xfagent";
            }

            if (channel != null) {
                try {
                    Map<String, Object> args = new HashMap<String, Object>();
                    args.put("x-max-priority", 255);
                    channel.queueDeclare(requestQueueName, true, false, false, args);
                } catch (IOException e) {
                    logger.error("Failed to declare agnet queue " + requestQueueName, e);
                }
            }
            xfagentMap.put(requestQueueName, replyToQueueName);
        }
    }

    private String getReplyQueueName() {
        String replyToQueueName = XFCommon.XFTaskReplyQueueName;

        if (workerQueueNameVersion == 1) {
            replyToQueueName = XFCommon.XFTaskReplyQueueNameV1;
        }
        return replyToQueueName;
    }

    @Override
    public void rpcHandleRequests() {
        try {
            String replyToQueueName = getReplyQueueName();

            String[] workerservernameArray = null;
            if (!StringUtils.isEmpty(workerserverNames))
            {
                workerservernameArray = StringUtils.trimAllWhitespace(workerserverNames).split(",");
            }

            if (channel != null) {
                channel.queueDeclare(replyToQueueName, true, false, false, null);

            }

            addWorkerservers(workerservernameArray);

            if (channel != null) {
                // avoid concurrency when consuming and acking messages, see https://www.rabbitmq.com/api-guide.html
                channel.basicQos(1);
                channel.basicConsume(replyToQueueName, false, new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope,AMQP.BasicProperties properties, byte[] body) throws IOException {
                        try {
                            if (!haSupport.isActive() || !dcSwitching.isActiveDC()) {
                                logger.debug("Noop in standby mode or inactive DC.");
                                channel.basicAck(envelope.getDeliveryTag(), false);
                                return;
                            }

                            String self_task_id = amqpClient.extractStringHeader(properties.getHeaders(), "self_task_id", null, true);
                            Integer taskResult = amqpClient.extractGenericHeader(properties.getHeaders(), XFCommon.XFTaskResult, 0, true);
                            String agentServerName = amqpClient.extractStringHeader(properties.getHeaders(), "serverName", "", false);
                            Optional<XFTask> xfTaskOpt = taskInMemoryRepository.findById(self_task_id);

                            if (body != null && body.length > 0)
                            {
                                String strBody = new String(body);
                                String strLog = "";
                                XFAgentHeartBeatMessage retHBMsg = CommonUtil.convertJsonStr2XFAgentHeartBeatMessage(strBody);
                                //XFAgent retXFAgent = XFAgent.convertJsonStr2XFAgentObject(strBody);
                                if (retHBMsg != null && retHBMsg.xfAgentInfo != null &&  StringUtils.isEmpty(retHBMsg.xfAgentInfo.getId()) == false )
                                {
                                    // Heartbeat things to do #1 - update XFAgentInMemoryRepository
                                    XFAgent rcvdXFAgent = retHBMsg.xfAgentInfo;
                                    XFAgent agengInfoInMem = xfAgentInMemoryRepository.GetOneXFAgent(rcvdXFAgent.getServerName());
                                    commonUtil.ProcessNewlyReceivedXFAgentInformation(agengInfoInMem, rcvdXFAgent, XFCommon.XFAgentInformationFrom.FROM_RABBITMQ_OTHER_MESSAGE);

                                    // Heartbeat things to do #2 - update XFTaskInMemoryRepository
                                    //commonUtil.ProcessNewlyReceivedXFTaskSummaryInfo(retHBMsg.selfTaskId2XFTaskSummaryDict);
                                }
                                else
                                {
                                    strLog = String.format("Failed to deserialize received heartbeat message embedded in ack message for taskid %s, from server %s, msgBody is \n %s",
                                            self_task_id, agentServerName, strBody);
                                    logger.warn(strLog);
                                }
                            }
                            if (xfTaskOpt.isPresent()) {

                                XFTask xfTask = xfTaskOpt.get();
                                String taskid = xfTask.getSelfTaskId();
                                // As long as we receive something from XFAgent, we need to delete the task from unacked list
                                logger.debug("flowengine received taskResult {} back from XFAgent {} for taskid {}.", taskResult, agentServerName, self_task_id);

                                xfAgentInMemoryRepository.deleteOneUnackedXFTaskInfo(agentServerName, taskid);

                                if (taskResult == Accept_Ok) {
                                    logger.info("flowengine received taskResult {} back from XFAgent {} for taskid {}.", taskResult, agentServerName, self_task_id);
                                } else if (taskResult == Accept_Canceled) {
                                    logger.debug("flowengine received taskResult {} back from XFAgent {} for taskid {}.", taskResult, agentServerName, self_task_id);
                                } else if (taskResult == Accept_DuplicateTask) {
                                    logger.debug("flowengine received taskResult {} back from XFAgent {} for taskid {}.", taskResult, agentServerName, self_task_id);
                                } else if (taskResult == Accept_Crashed) {
                                    logger.debug("flowengine received taskResult {} back from XFAgent {} for taskid {}.", taskResult, agentServerName, self_task_id);
                                } else if (taskResult == Accept_UnknownContentType) {
                                    logger.debug("flowengine received taskResult {} back from XFAgent {} for taskid {}.", taskResult, agentServerName, self_task_id);
                                } else if (taskResult == Accept_Exception) {
                                    logger.debug("flowengine received taskResult {} back from XFAgent {} for taskid {}.", taskResult, agentServerName, self_task_id);
                                } else if (taskResult == Accept_NoQueueExist) {
                                    logger.debug("flowengine received taskResult {} back from XFAgent {} for taskid {}.", taskResult, agentServerName, self_task_id);
                                } else if (taskResult > NEED_REDISPATCH) {
                                    // if we received this, it indicate that this task has not been actually executed, so we should subtract the taskcount for it.
                                    XFAgentMetadata selectedMetadata = xfAgentInMemoryRepository.GetOneXFAgentMetadata(agentServerName);
                                    if (selectedMetadata != null) {
                                        selectedMetadata.setRecentTaskCount(selectedMetadata.getRecentTaskCount() - 1);
                                    }

                                    TaskRequest taskRequest = new TaskRequest(xfTaskOpt.get(), null);
                                    taskQueue.add(taskRequest);
                                    logger.debug("Requeueing task because flowengine received taskResult {} back from XFAgent {} for taskid {}.", taskResult, agentServerName, self_task_id);
                                }
                            }
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } catch (Exception e) {
                            logger.error("Exception occurred when processing reply-to message from XFAgent", e);
                        }
                    }
                });
            }
        } catch (IOException e) {
            logger.error("Failed to setup consumer to messaging server", e);
        }
    }
}
