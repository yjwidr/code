package com.netbrain.xf.flowengine.gateway;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;

import com.netbrain.xf.flowengine.config.FlowEngineConfig;
import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskflowInMemoryRepository;
import com.netbrain.xf.flowengine.utility.CommonUtil;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.utility.HASupport;
import com.netbrain.xf.model.XFTaskflow;
import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.netbrain.xf.flowengine.scheduler.services.ISchedulerServices;
import com.netbrain.xf.flowengine.taskcontroller.SubmitTaskResult;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.xfcommon.XFCommon;
import com.netbrain.xf.flowengine.workerservermanagement.*;

import javax.swing.text.html.Option;

@Component
public class AMQPTaskGateway implements ITaskGateway {
    private static Logger logger = LogManager.getLogger(AMQPTaskGateway.class.getSimpleName());

    public static final String TASK_TYPE_HEADER_KEY = "task_message_content_type";
    public static final String TASK_TYPE_NEW_TASK = "task_message_content_type_task";
    public static final String TASK_TYPE_NEW_SUB_TASK = "task_message_content_type_sub_task";
    public static final String TASK_TYPE_NEW_NEXT_TASK = "task_message_content_type_next_task";
    public static final String TASK_TYPE_TASK_BEGIN = "task_message_content_type_execution_begin";
    public static final String TASK_TYPE_TASK_END = "task_message_content_type_execution_end";

    private static final String TASK_EXCHANGE_NAME = "ready_tasks_exchange";
    private static final String TASK_QUEUE_NAME = "prepared_tasks";
    private static final String CONSUMER_TAG = "flow-engine-task-gw";

    private static final String CLI_TO_SCHEDULER_EXCHANGE_NAME = "scheduler_exchange";
    private static final String CLI_TO_SCHEDULER_QUEUE_NAME = "rmclient2scheduler_queue";

    private static final String XFAGENT_EXCHANGE = "nb_xfagent_notification_exchange";
    private static final String TASK_STATUS_ROUTING_KEY = "task_status";
    private static final String TASK_STATUS_QUEUE_NAME = "nb_flowengine_task_status_queue";

    private static final String HEALTH_MONITOR_QUEUE_NAME_FORMAT = "nb_flowengine_health_monitor_queue";
    private static final String HEALTH_MONITOR_QUEUE_ROUTING_KEY = "nb_flowengine_health_monitor_routingkey";

    @Autowired
    AMQPClient amqpClient;

    @Autowired
    private TaskController taskController;
    @Autowired
    private ISchedulerServices  schedulerServices;

    @Autowired
    private TaskStatusMessageHandler taskStatusMessageHandler;

    @Autowired
    private WorkerServerManagementMessageHandler workerServerManagementMessageHandler;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private FlowEngineConfig flowEngineConfig;

    @Autowired
    private HASupport haSupport;

    @Autowired
    private XFTaskInMemoryRepository taskInMemoryRepository;

    @Autowired
    private XFTaskflowInMemoryRepository taskflowInMemoryRepository;

    @Autowired
    private XFTaskflowRepository taskflowRepository;

    @Autowired
    private XFTaskRepository taskRepository;

    @Autowired
    private DataCenterSwitching dcSwitching;

    private Channel prepareTaskChannel;
    private Channel clientToSchedulerChannel;
    private Channel taskStatusChannel;
    private Channel healthMonitorChannel;

    // For testing purpose only
    protected void setTaskController(TaskController taskController) {
        this.taskController = taskController;
    }

    @Override
    public int initListener() {
        Connection mqConnection = amqpClient.getMqConnection();
        try {
            if (mqConnection != null) {
                prepareTaskChannel = mqConnection.createChannel();
                clientToSchedulerChannel = mqConnection.createChannel();
                taskStatusChannel = mqConnection.createChannel();
                healthMonitorChannel = mqConnection.createChannel();

//                prepareTaskChannel.addShutdownListener(new ShutdownListener() {
//                    @Override
//                    public void shutdownCompleted(ShutdownSignalException cause) {
//                        Method reason = cause.getReason();
//                        if (!cause.isHardError()) {
//                            // channel level error
//                            Channel closedChannel = (Channel)cause.getReference();
//                            logger.info("!!!!Channel " + closedChannel.toString() + " is closed due to " + reason);
//
//                        } else {
//                            Connection closedConnection = (Connection)cause.getReference();
//                            logger.info("!!!!Connection " + closedConnection.toString() + " is closed due to " + reason);
//                        }
//                    }
//                });
            } else {
                return -1;
            }
        } catch (IOException e) {
            logger.error("Failed to create channels to messaging server", e);
        }
        return 0;
    }

    // if the child XFTask has been successfully saved to DB by RMClient, no need to re-create it from rabbitmq message
    // also in the DB the xftask's submittime etc. is more accurate.
    protected XFTask generateChildXFTask(AMQP.BasicProperties properties, byte[] body) {
        Map<String, Object> headers = properties.getHeaders();
        String selftaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_SELF_TASK_ID, "", true);

        XFTask retChildXFTask = null;
        Optional<XFTask> childTaskOpt = this.taskInMemoryRepository.findById(selftaskId);
        if (childTaskOpt.isPresent()) {
            retChildXFTask = childTaskOpt.get();
        }
        else
        {
            retChildXFTask = generateXFTask(properties, body);
        }

        return retChildXFTask;

    }
    protected XFTask generateXFTask(AMQP.BasicProperties properties, byte[] body) {
        Map<String, Object> headers = properties.getHeaders();

        XFTask task = new XFTask();
        String roottaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_ROOT_TASK_ID, null, true);
        String parentaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_PARENT_TASK_ID, "", false);
        String selftaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_SELF_TASK_ID, roottaskId, false);
        String taskflowId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_TASKFLOW_ID, null, false);
        List<String> associatedDtgIds = amqpClient.extractGenericHeader(headers, XFCommon.MSG_KEY_ASSOC_DTG_IDS, new ArrayList<String>(), false);
        String ancestorTaskIds = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_ANCESTOR_TASK_IDS, "", false);
        if (selftaskId == null || selftaskId.isEmpty())
        {
            selftaskId = roottaskId;
            task.setId(roottaskId);
        }
        else
        {
            task.setId(selftaskId);
        }

        String strMessageTTLInMS = properties.getExpiration();
        if (strMessageTTLInMS != null)
        {
            try {
                // add an extra 10000ms with a small delta so that a task expires after Web layer times out
                // see NBRMClient.cs, search callback.SetOnBegin(DateTime.Now.AddMilliseconds(timeoutOfStart + 10000), (a, e) =>
                long ttlInMS = Long.parseLong(strMessageTTLInMS) + (10000 + 100);
                Instant taskExpire = Instant.now().plusMillis(ttlInMS);
                task.setExpireTime(taskExpire);
            }
            catch (Exception e)
            {
                logger.warn("Failed to parseLong for the Expiration string {} for task {}" ,strMessageTTLInMS, selftaskId);
                task.setExpireTime(null);
            }
        }

        task.setRootTaskId(roottaskId);
        task.setParentTaskId(parentaskId);
        //set the Taskflow id for now, taskController will set the taskflow object
        task.setTaskflowId(taskflowId);

        task.setJobId(amqpClient.extractStringHeader(headers, "task_job_id", null, true));
        task.setShortDescription(amqpClient.extractStringHeader(headers, "shortDescription", "", false));
        task.setJobRunCategory(amqpClient.extractStringHeader(headers, "jobRunCategory", "", false));
        task.setTaskType(amqpClient.extractStringHeader(headers, "task_type", "", true));
        task.setTaskPriority(amqpClient.extractGenericHeader(headers, "task_priority", 10, true));

        task.setTaskLevelFromRoot(amqpClient.extractGenericHeader(headers, "taskLevelFromRoot", 0, false));

        int rmqPriority = XFCommon.TASK_RABBITMQ_PRIORITY_LOW;
        if (properties.getPriority() != null)
        {
            rmqPriority = properties.getPriority().intValue();
        }
        else
        {
            int taskPrio = task.getTaskPriority();
            if (taskPrio == XFCommon.TASK_PRIORITY_SUPER)
            {
                rmqPriority = XFCommon.TASK_RABBITMQ_PRIORITY_SUPER;
            }
            else if (taskPrio == XFCommon.TASK_PRIORITY_HIGH)
            {
                rmqPriority = XFCommon.TASK_RABBITMQ_PRIORITY_HIGH;
            }
            else
            {
                rmqPriority = XFCommon.TASK_RABBITMQ_PRIORITY_LOW;
            }
        }
        task.setTaskRabbitmqPriority(rmqPriority);


        task.setUserName(amqpClient.extractStringHeader(headers, "user_name", "", false));
        task.setUserIP(amqpClient.extractStringHeader(headers, "user_IPAddress", "", false));
        //some task no db info
        task.setTenantId(amqpClient.extractStringHeader(headers, "tenantId", "", false));
        task.setTenantDbName(amqpClient.extractStringHeader(headers, "tenantDbName", "", false));
        task.setDomainId(amqpClient.extractStringHeader(headers, "domainId", "", false));
        task.setDomainDbName(amqpClient.extractStringHeader(headers, "domainDbName", "", false));

        task.setWorkerRestartTimes(amqpClient.extractGenericHeader(headers, "WorkerRestartTimes", -2, false));
        task.setTaskCallbackQueue(amqpClient.extractStringHeader(headers, "task_callback_queue", "", false));
        task.setNeedBroadCallbackToAllApiServer(amqpClient.extractGenericHeader(headers, "needBroadCallbackToAllApiServer", false, false));

        task.setTaskParameters(new String(body));

        task.setAssociatedDtgIds(associatedDtgIds);
        task.setMaterializedPathToParent(ancestorTaskIds);
        return task;
    }

    protected SubmitTaskResult handleNewTask(AMQP.BasicProperties properties, byte[] body) {
        XFTask xftaskobj = generateXFTask(properties, body);

        int currTaskCount = this.taskInMemoryRepository.getXFTaskCount();
        // if there are already too many task in memory, we should save the new task in DB and not in memory.
        // the task saved into DB will be picked up later by a quartz job to execute it if at that time there are enough resources, etc.
        if (currTaskCount >= commonUtil.maxTaskCountLimitInConfig )
        {
            String strLog =
                    String.format("TaskEngine on %s received new task %s but its task count %d reached limit %d, and thus will be put on the waiting list in DB",
                    commonUtil.localHostName, xftaskobj.getId(), currTaskCount, commonUtil.maxTaskCountLimitInConfig);
            logger.warn(strLog);

            boolean bDBOK = false;
            boolean bTaskcountBelowLimit = false;
            while (true)
            {
                try {

                    String taskFlowId = xftaskobj.getRootTaskId();
                    XFTaskflow xfTaskflow = new XFTaskflow();
                    xfTaskflow.setId(taskFlowId);
                    xfTaskflow.setJobId(xftaskobj.getJobId());
                    XFTask savedXfTaskObj = null;
                    XFTaskflow savedxfTaskflow = null;

                    xftaskobj.setXfTaskflow(xfTaskflow);
                    try
                    {
                        savedXfTaskObj = taskRepository.save(xftaskobj);
                    }
                    catch(Exception e)
                    {
                        strLog =
                                String.format("TaskEngine on %s received new task %s but its task count %d reached limit %d, try to save task into DB and failed, ",
                                        commonUtil.localHostName, xftaskobj.getId(), currTaskCount, commonUtil.maxTaskCountLimitInConfig);
                        logger.warn(strLog, e);
                    }

                    // we try to save taskflow only when we successfully saved the task, we never want flow exist before its task's inception
                    if (savedXfTaskObj != null)
                    {
                        savedxfTaskflow = taskflowRepository.save(xfTaskflow);
                    }
                    if (savedxfTaskflow != null)
                    {
                        bDBOK = true;
                        return SubmitTaskResult.OK;
                    }
                }
                catch(Exception e)
                {
                    strLog =
                            String.format("TaskEngine on %s received new task %s but its task count %d reached limit %d, try to save task and taskflow into DB and failed, will retry",
                                    commonUtil.localHostName, xftaskobj.getId(), currTaskCount, commonUtil.maxTaskCountLimitInConfig);
                    logger.warn(strLog, e);
                }

                // if failure to save to DB but there is available space in memory, insert it into memory directly.
                // this might cause this task to be sent to XFAgent earlier than those aleady waiting in DB buffer, but that's ok. XF does not guarantee ordered delivery in the first place.
                currTaskCount = this.taskInMemoryRepository.getXFTaskCount();
                bTaskcountBelowLimit = (currTaskCount < commonUtil.maxTaskCountLimitInConfig);
                if (bTaskcountBelowLimit == true)
                {
                    SubmitTaskResult result=taskController.submitTask(xftaskobj, properties);
                    return result;
                }

                int sleepTime = 5 * 1000;
                try {

                    Thread.sleep(sleepTime);
                } catch (Exception se) {
                    logger.warn("Exception happened while sleeping " + sleepTime + " during currTaskCount >= this.maxTaskCountLimitInConfig " , se);
                }
            }// end of while
        } // end of if
        else
        {
            SubmitTaskResult result=taskController.submitTask(xftaskobj, properties);
            return result;
        }
    }

    protected SubmitTaskResult handleNewSubTask(AMQP.BasicProperties properties, byte[] body) {
        XFTask xftaskobj = generateChildXFTask(properties, body);
        return taskController.submitChildOrNextTask(xftaskobj, properties);
    }

    protected SubmitTaskResult handleNewNextTask(AMQP.BasicProperties properties, byte[] body) {
        XFTask xftaskobj = generateXFTask(properties, body);
        return taskController.submitChildOrNextTask(xftaskobj, properties);
    }

    protected boolean handleStopTaskflow(AMQP.BasicProperties properties, byte[] body) {
        Map<String, Object> headers = properties.getHeaders();
        int timeOut = amqpClient.extractGenericHeader(headers, "task_message_content_option", 1, false);
        String strCancelReason = amqpClient.extractStringHeader(headers, XFCommon.cancel_task_reason, "", false);
        String strJobIdOrTaskflowId = new String(body);

        boolean stopped = false;
        boolean isJobId = commonUtil.checkIsValidExistingJobId(strJobIdOrTaskflowId);
        if (isJobId)
        {
            logger.warn("Received Stop/Cancelrequest for jobid={}, timeout={}", strJobIdOrTaskflowId, timeOut);
            stopped = taskController.stopTaskflowByJobIdOrTaskflowId(strJobIdOrTaskflowId, isJobId, timeOut,strCancelReason);

        }
        else
        {
            logger.warn("Received Stop/Cancel request for taskflowId={}, timeout={}", strJobIdOrTaskflowId, timeOut);
            stopped = taskController.stopTaskflowByJobIdOrTaskflowId(strJobIdOrTaskflowId, isJobId, timeOut,strCancelReason);
        }

        if (!stopped) {
            // TODO: send status back to XFClient
            logger.warn("Failed to stop task, strJobIdOrTaskflowId=" + strJobIdOrTaskflowId);
        }else{
            logger.info("Succeeded to stop task, strJobIdOrTaskflowId=" + strJobIdOrTaskflowId);
        }
        return stopped;
    }

    protected boolean handleAddScheduledJob(AMQP.BasicProperties properties, byte[] body) {
        String strJobId = new String(body);
        schedulerServices.addScheduledJob(strJobId);
        return true;
    }

    protected boolean handleDelScheduledJob(AMQP.BasicProperties properties, byte[] body) {
        String strJobId = new String(body);
        schedulerServices.deleteJob(strJobId);
        return true;
    }

    protected boolean handleRunNowTaskflow(String jobId ) {
        if(!StringUtils.isEmpty(jobId)) {
            return schedulerServices.runNow(jobId);
        }
        return false;
    }

    private String getUniqueConsumerTag() {
        return amqpClient.getUniqueConsumerTag(CONSUMER_TAG);
    }

    protected void setUpPrepareTaskConsumer(String queueName, Channel channel) throws IOException {
        channel.basicConsume(queueName, false, getUniqueConsumerTag(), new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {
                logger.info("Received a task request from RMClient on " + queueName + "" +
                        ", redeliver? " + envelope.isRedeliver());
                try {

                    if(!dcSwitching.isActiveDC()){
                        logger.debug("Noop in inactive DC.");
                        // This message should be acknowledged in inactive data center
                        channel.basicAck(envelope.getDeliveryTag(), false);
                        return;
                    }

                    if (!haSupport.isActive() || !dcSwitching.isActiveDC()) {
                        logger.debug("Noop in standby mode or inactive DC");
                        // TODO: consider changing this to basicReject
                        channel.basicAck(envelope.getDeliveryTag(), false);
                        return;
                    }

                    Map<String, Object> headers = properties.getHeaders();
                    if (headers != null) {
                        String taskMessageContentType = amqpClient.extractStringHeader(properties.getHeaders(), TASK_TYPE_HEADER_KEY, "", true);

                        if (TASK_TYPE_NEW_TASK.equals(taskMessageContentType)) {
                            String roottaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_ROOT_TASK_ID, null, true);
                            String parentaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_PARENT_TASK_ID, "", false);
                            String selftaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_SELF_TASK_ID, roottaskId, false);
                            String taskflowId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_TASKFLOW_ID, null, false);

                            // do some duplicate checks
                            boolean bExists = commonUtil.checkTaskExistence(selftaskId);
                            if (bExists)
                            {
                                logger.warn("Flowengine Taskgateway received a duplicated task: content={} for selftaskId= {}, will be ignored", taskMessageContentType, selftaskId);
                                channel.basicAck(envelope.getDeliveryTag(), false);
                                return;
                            }

                            logger.info("Flowengine Taskgateway received a content={} for selftaskId= {}", taskMessageContentType, selftaskId);
                            SubmitTaskResult result = handleNewTask(properties, body);
                            if (result != SubmitTaskResult.OK) {
                                // send the result back to XFClient so that users have more clues
                                logger.warn("Failed to submit a task {}, result {}", selftaskId, result);
                                channel.basicReject(envelope.getDeliveryTag(), true);
                                return;
                            }

                        } else if (TASK_TYPE_NEW_SUB_TASK.equals(taskMessageContentType)) {
                            String roottaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_ROOT_TASK_ID, null, true);
                            String parentaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_PARENT_TASK_ID, "", false);
                            String selftaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_SELF_TASK_ID, roottaskId, false);
                            String taskflowId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_TASKFLOW_ID, null, false);

                            logger.info("Flowengine Taskgateway received a content={} for selftaskId= {}", taskMessageContentType, selftaskId);
                            SubmitTaskResult result = handleNewSubTask(properties, body);
                            if (result != SubmitTaskResult.OK) {
                                // send the result back to XFClient so that users have more clues
                                logger.warn("Failed to submit a sub task {}, result {}", selftaskId, result);
                                channel.basicReject(envelope.getDeliveryTag(), true);
                                return;
                            }
                        }else if(TASK_TYPE_NEW_NEXT_TASK.equals(taskMessageContentType)){
                            String roottaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_ROOT_TASK_ID, null, true);
                            String parentaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_PARENT_TASK_ID, "", false);
                            String selftaskId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_SELF_TASK_ID, roottaskId, false);
                            String taskflowId = amqpClient.extractStringHeader(headers, XFCommon.MSG_KEY_TASKFLOW_ID, null, false);
                            
                            logger.info("Flowengine Taskgateway received a content={} for selftaskId= {}", taskMessageContentType, selftaskId);
                            SubmitTaskResult result = handleNewNextTask(properties, body);
                            if (result != SubmitTaskResult.OK) {
                                // send the result back to XFClient so that users have more clues
                                logger.warn("Failed to submit a next task {}, result {}", selftaskId, result);
                                channel.basicReject(envelope.getDeliveryTag(), true);
                                return;
                            }
                        }
                        else {
                            logger.warn("Unknown task message content type " + taskMessageContentType);
                        }
                    } else {
                        logger.warn("Missing message header for a message received on queue " + queueName);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Bad message received on queue " + queueName, e);
                } catch (Exception e) {
                    logger.warn("Failed to process message received on queue " + queueName, e);
                    channel.basicReject(envelope.getDeliveryTag(), true);
                    return;
                }

                try
                {
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
                catch (Exception e) {
                    logger.warn("Failed to do channel.basicAck for tag " + envelope.getDeliveryTag() + " received on queue " + queueName, e);
                }
            }
        });
    }

    public void setUpClientToSchedulerConsumer(String queueName, Channel channel) throws IOException {
        channel.basicConsume(queueName, false, getUniqueConsumerTag(), new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {
                logger.info("Received a task request from RMClient on " + queueName +
                        ", redeliver? " + envelope.isRedeliver());
                try {
                    channel.basicAck(envelope.getDeliveryTag(), false);

                    if (!haSupport.isActive() || !dcSwitching.isActiveDC()) {
                        logger.debug("Noop in standby mode or inactive DC.");
                        return;
                    }

                    Map<String, Object> headers = properties.getHeaders();
                    if (headers != null) {
                        int taskMessageContentType = amqpClient.extractGenericHeader(properties.getHeaders(),
                                TASK_TYPE_HEADER_KEY, 0, true);
                        if (ETaskOp.Task_Stop.getIntValue() == taskMessageContentType) {
                            logger.info("Received task message content type ETaskOp.Task_Stop.");
                            handleStopTaskflow(properties, body);
                        } else if(ETaskOp.Task_Add.getIntValue() == taskMessageContentType) {
                            logger.info("Received task message content type ETaskOp.Task_Add");
                            handleAddScheduledJob(properties, body);
                        } else if(ETaskOp.Task_Del.getIntValue() == taskMessageContentType) {
                            logger.info("Received task message content type ETaskOp.Task_Del");
                            handleDelScheduledJob(properties, body);
                        } else if(ETaskOp.Task_Run.getIntValue() == taskMessageContentType) {
                                String jobId = new String(body);
                                logger.info("Received task message content type ETaskOp.Task_Run, jobId: " + jobId);
                                if(!handleRunNowTaskflow(jobId)) {
                                    logger.warn("runNow failed by jobId: {}",jobId);
                                }
                        }else  {
                            logger.warn("Unknown task message content type " + taskMessageContentType);
                        }
                    } else {
                        logger.warn("Missing message header for a message received on queue " + queueName);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Bad message received on queue " + queueName, e);
                } catch (Exception e) {
                    logger.warn("Failed to process message received on queue " + queueName, e);
                }
            }
        });
    }

    protected void setupTaskStatusConsumer(String queueName, Channel channel) throws IOException {
        channel.basicConsume(queueName, false, getUniqueConsumerTag(), new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {
                taskStatusMessageHandler.handleMessage(queueName, channel, consumerTag, envelope, properties, body);
            }
        });
    }

    protected void setupHealthMonitorConsumer(String queueName, Channel channel, boolean bAutoAck ) throws IOException {

        channel.basicConsume(queueName, bAutoAck, getUniqueConsumerTag(), new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException
            {
                workerServerManagementMessageHandler.handleMessage(queueName, channel, consumerTag, envelope, properties, body);
            }
        });
    }



    @Override
    public void handleRequests() {
        try {
            if (prepareTaskChannel != null) {
                prepareTaskChannel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
                // avoid concurrency when consuming and acking messages, see https://www.rabbitmq.com/api-guide.html
                prepareTaskChannel.basicQos(1);
                setUpPrepareTaskConsumer(TASK_QUEUE_NAME, prepareTaskChannel);
            }

            if (clientToSchedulerChannel != null) {
                clientToSchedulerChannel.exchangeDeclare(CLI_TO_SCHEDULER_EXCHANGE_NAME, "direct", true);
                clientToSchedulerChannel.queueDeclare(CLI_TO_SCHEDULER_QUEUE_NAME, true, false, false, null);
                clientToSchedulerChannel.queueBind(CLI_TO_SCHEDULER_QUEUE_NAME, CLI_TO_SCHEDULER_EXCHANGE_NAME, CLI_TO_SCHEDULER_QUEUE_NAME);
                // avoid concurrency when consuming and acking messages, see https://www.rabbitmq.com/api-guide.html
                clientToSchedulerChannel.basicQos(1);
                setUpClientToSchedulerConsumer(CLI_TO_SCHEDULER_QUEUE_NAME, clientToSchedulerChannel);
            }

            if (taskStatusChannel != null) {
                // XFAgent does the following setup as well
                taskStatusChannel.exchangeDeclare(XFAGENT_EXCHANGE, XFCommon.RabbitMqString.EXCHANGE_TYPE_TOPIC, true);
                taskStatusChannel.queueDeclare(TASK_STATUS_QUEUE_NAME, true, false, false, null);
                taskStatusChannel.queueBind(TASK_STATUS_QUEUE_NAME, XFAGENT_EXCHANGE, TASK_STATUS_ROUTING_KEY);
                // avoid concurrency when consuming and acking messages, see https://www.rabbitmq.com/api-guide.html
                taskStatusChannel.basicQos(1);
                setupTaskStatusConsumer(TASK_STATUS_QUEUE_NAME, taskStatusChannel);
            }

            if (healthMonitorChannel != null) {
                healthMonitorChannel.exchangeDeclare(XFCommon.FLOWENGINE_EXCHANGE, XFCommon.RabbitMqString.EXCHANGE_TYPE_TOPIC, true);
                healthMonitorChannel.queueDeclare(HEALTH_MONITOR_QUEUE_NAME_FORMAT, true, false, false, null);
                healthMonitorChannel.queueBind(HEALTH_MONITOR_QUEUE_NAME_FORMAT, XFCommon.FLOWENGINE_EXCHANGE, HEALTH_MONITOR_QUEUE_ROUTING_KEY);
                // avoid concurrency when consuming and acking messages, see https://www.rabbitmq.com/api-guide.html
                healthMonitorChannel.basicQos(1);
                boolean bAutoAck = true; // We can afford losing some message, so why not set autoack to true?
                bAutoAck = false;
                setupHealthMonitorConsumer(HEALTH_MONITOR_QUEUE_NAME_FORMAT, healthMonitorChannel, bAutoAck);
            }
        } catch (IOException e) {
            logger.error("Failed to setup consumer to messaging server", e);
        }
    }
}
