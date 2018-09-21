package com.netbrain.xf.flowengine.fscclient;

import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.netbrain.xf.flowengine.config.FlowEngineConfig;
import com.netbrain.xf.flowengine.dao.XFDtgRepository;
import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskflowInMemoryRepository;
import com.netbrain.xf.flowengine.metric.Metrics;
import com.netbrain.xf.flowengine.taskcontroller.SubmitTaskResult;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.flowengine.utility.CommonUtil;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.utility.HASupport;
import com.netbrain.xf.model.XFDtg;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import com.netbrain.xf.xfcommon.XFCommon;
import com.rabbitmq.client.*;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

/**
 * Receive trigger events sent by Front Server Controller
 */
@Component
public class AMQPTriggerReceiver {
    private static Logger logger = LogManager.getLogger(AMQPTriggerReceiver.class.getSimpleName());

    private static final String CONSUMER_TAG = "flow-engine-trigger-receiver";

    public static final String TRIGGER_MSG_ID = "id";
    public static final String TRIGGER_MSG_DTGID = "taskgroupid";
    public static final String TRIGGER_MSG_TYPE = "type";
    public static final String TRIGGER_MSG_FINAL = "is_final_trigger";
    public static final String TRIGGER_MSG_TS = "timestamp";

    @Value("${trigger.receiver.username}")
    String submitUsername;

    @Autowired
    AMQPClient amqpClient;

    @Autowired
    private TaskController taskController;

    @Autowired
    private XFDtgRepository dtgRepository;

    @Autowired
    private XFTaskflowInMemoryRepository taskflowInMemoryRepository;

    @Autowired
    private XFTaskRepository taskRepository;

    @Autowired
    private XFTaskInMemoryRepository taskInMemoryRepository;

    private Channel channel;

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

    // Expose several setters for testing purpose
    protected void setDtgRepository(XFDtgRepository dtgRepository) {
        this.dtgRepository = dtgRepository;
    }

    protected void setTaskflowRepository(XFTaskflowInMemoryRepository taskflowRepository) {
        this.taskflowInMemoryRepository = taskflowRepository;
    }

    protected void setTaskInMemoryRepository(XFTaskInMemoryRepository taskInMemoryRepository) {
        this.taskInMemoryRepository = taskInMemoryRepository;
    }

    // For testing purpose only
    protected void setTaskController(TaskController taskController) {
        this.taskController = taskController;
    }

    @PostConstruct
    public int initConsumer() {
        Connection mqConnection = amqpClient.getMqConnection();
        String routingKey = "notice";

        try {
            if (mqConnection != null) {
                channel = mqConnection.createChannel();

                String queueNameFormatter = XFCommon.TRIGGER_QUEUE_NAME_V1;
                channel.exchangeDeclare(XFCommon.TRIGGER_EXCHANGE_NAME, "direct", true);
                channel.queueDeclare(queueNameFormatter, true, false, false, null);
                channel.queueBind(queueNameFormatter, XFCommon.TRIGGER_EXCHANGE_NAME, routingKey);
                // avoid concurrency when consuming and acking messages, see https://www.rabbitmq.com/api-guide.html
                channel.basicQos(1);
                String queueName = queueNameFormatter;
                channel.basicConsume(queueName, false, amqpClient.getUniqueConsumerTag(CONSUMER_TAG), new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag,
                                               Envelope envelope,
                                               AMQP.BasicProperties properties,
                                               byte[] body)
                            throws IOException {
                        logger.info("Received a trigger event message on " + queueName +
                                ", redeliver? " + envelope.isRedeliver());
                        try {

                            if(!dcSwitching.isActiveDC()){
                                logger.debug("Noop in inactive DC.");
                                // This message should be acknowledged in inactive data center
                                channel.basicAck(envelope.getDeliveryTag(), false);
                                return;
                            }
                            if (!haSupport.isActive()) {
                                logger.debug("Noop in standby mode.");
                                // Consider using basicReject to handle message
                                channel.basicAck(envelope.getDeliveryTag(), false);
                                return;
                            }

                            metrics.addTriggerCount(1);

                            Map<String, Object> headers = properties.getHeaders();
                            if (headers != null) {
                                handleTriggerEvent(properties, body);
                            } else {
                                logger.warn("Missing message header for a message received on queue " + queueName);
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warn("Bad message received on queue " + queueName, e);
                        } catch (Exception e) {
                            logger.warn("Failed to process message received on queue " + queueName, e);
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
            } else {
                return -1;
            }
        } catch (IOException e) {
            logger.error("Failed to create channels to messaging server", e);
        }
        return 0;
    }

    private void fillPropertiesFromDtg(XFTask task, XFDtg dtg) {
        task.setTaskType(dtg.getTriggeredTaskType());
        task.setTaskParameters(dtg.getTriggeredTaskParameters());
        task.setDtgId(dtg.getId());

        List<String> associatedDtgIds = new ArrayList<String>();
        associatedDtgIds.addAll(dtg.getAncestorDtgIds());
        associatedDtgIds.add(dtg.getId());
        task.setAssociatedDtgIds(associatedDtgIds);
    }

    // Inherit a bunch of properties from XFTaskflow's initiating task
    private void fillPropertiesFromTaskflow(XFTask task, XFTask initTask, XFTaskflow taskflow) {
        task.setXfTaskflow(taskflow);
        task.setJobId(initTask.getJobId());
        task.setShortDescription(initTask.getShortDescription());
        task.setJobRunCategory(initTask.getJobRunCategory());
        task.setTaskPriority(initTask.getTaskPriority());

        int rmqPriority = XFCommon.TASK_RABBITMQ_PRIORITY_LOW;
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
        task.setTaskRabbitmqPriority(rmqPriority);

        // triggered task are treated as submitted by the user of its seed task
        task.setUserName(initTask.getUserName());
        task.setTenantId(initTask.getTenantId());
        task.setTenantDbName(initTask.getTenantDbName());
        task.setDomainId(initTask.getDomainId());
        task.setDomainDbName(initTask.getDomainDbName());
        task.setWorkerRestartTimes(initTask.getWorkerRestartTimes());
        task.setTaskCallbackQueue(initTask.getTaskCallbackQueue());
        task.setNeedBroadCallbackToAllApiServer(initTask.isNeedBroadCallbackToAllApiServer());

        // trigger tasks are root tasks
        task.setRootTaskId(task.getSelfTaskId());

        String strParenttaskId = task.getParentTaskId();
        if (StringUtils.isEmpty(strParenttaskId))
        {
            task.setTaskLevelFromRoot(0);
        }
    }

    protected XFTask generateXFTask(String triggerId, String dtgId, boolean isFinal) {
        XFTask task = new XFTask();

        String taskId = UUID.randomUUID().toString();
        task.setId(taskId);
        // this is overriden by seedtask's username in later processing
        task.setUserName(submitUsername);
        task.setTriggerId(triggerId);
        task.setFinalTrigger(isFinal);
        try {
            task.setUserIP(InetAddress.getLocalHost().getHostAddress().trim());
        } catch (UnknownHostException e) {
            task.setUserIP("127.0.0.1");
        }

        Optional<XFDtg> dtgOptional = dtgRepository.findById(dtgId);
        if (dtgOptional.isPresent()) {
            // Figure out task type and params from registered trigger
            XFDtg dtg = dtgOptional.get();

            if(dtg.isFinalTriggerReceived() == true) {
                logger.info("Ignoring trigger event ID after DTG {} (Taskflow ID: {}) has received final trigger",
                        dtg.getId(), dtg.getTaskflowId());
                return null;
            } else if (dtg.hasFinished()) {
                logger.info("Ignoring trigger event ID since DTG {} (Taskflow ID: {}) has completed",
                        dtg.getId(), dtg.getTaskflowId());
                return null;
            } else {
                dtgRepository.incTriggerReceivedTotalTimes(dtg, 1);

                if(isFinal){
                    dtgRepository.updateFinalTriggerReceived(dtg, isFinal);
                }

                // we need to get the DTG from database again after updating some fields
                // This dtgId must exist here since we have checked its existence a couple lines above
                dtg = dtgRepository.findById(dtgId).get();
                fillPropertiesFromDtg(task, dtg);

                Optional<XFTask> seedTaskOptional = taskInMemoryRepository.findById(dtg.getTaskflowId());
                Optional<XFTaskflow> taskFlowOptional = taskflowInMemoryRepository.findById(dtg.getTaskflowId(), true, true);
                if (seedTaskOptional.isPresent() && taskFlowOptional.isPresent()) {
                    XFTaskflow taskflow = taskFlowOptional.get();
                    if (taskflow.getStatusIsFinal()) {
                        logger.info("Ignoring trigger event ID Taskflow ID: {} has completed", dtg.getTaskflowId());
                        return null;
                    }

                    if (taskflow.isStopRequested()) {
                        logger.info("Trigger event belongs to a stopping Taskflow {}, stop this DTG.", dtg.getTaskflowId());
                        taskController.stopDtgByJobIdOrTaskflowId(dtg.getTaskflowId(), false, "Taskflow is being stopped");
                        taskController.CalculateTasklowStatus(dtg.getTaskflowId(), true);
                        return null;
                    }

                    fillPropertiesFromTaskflow(task, seedTaskOptional.get(), taskFlowOptional.get());
                    return task;
                } else {
                    logger.warn("Ignoring unknown Taskflow ID " + dtg.getTaskflowId());
                    return null;
                }
            }
        } else {
            logger.warn("Ignoring unknown DTG ID " + dtgId);
            return null;
        }
    }

    protected SubmitTaskResult handleTriggerEvent(AMQP.BasicProperties properties, byte[] body) {
        String triggerId = amqpClient.extractStringHeader(properties.getHeaders(), TRIGGER_MSG_ID, "", true);
        String dtgId = amqpClient.extractStringHeader(properties.getHeaders(), TRIGGER_MSG_DTGID, "", true);
        boolean isFinal = amqpClient.extractGenericHeader(properties.getHeaders(), TRIGGER_MSG_FINAL, false, false);
        String triggerGenTime = amqpClient.extractStringHeader(properties.getHeaders(), TRIGGER_MSG_TS, "", false);

        String strLog =
                String.format("Received triggered event(dtgId=%s, triggerId=%s, isFinalTrigger=%b)", dtgId, triggerId, isFinal);
        logger.info(strLog);
        return generateAndSubmitTask(triggerId, dtgId, isFinal);
    }

    private boolean checkIfTriggeredTaskShouldContinue(XFTask xfTask)
    {
        boolean bContinue = true;
        boolean isFinalTrigger = xfTask.isFinalTrigger();
        String taskId = xfTask.getId();
        String taskflowId = xfTask.getTaskflowId();
        String dtgId = xfTask.getDtgId();
        String triggerId = xfTask.getTriggerId();

        try
        {
            if(isFinalTrigger) {
                String strLog =
                        String.format("Final DTG triggered task(selfTaskId=%s, taskflowId=%s, dtgId=%s, triggerId=%s) will not be ignored regardless if the previous task(s) for the same dtgId has not finished yet",
                                taskId, taskflowId, dtgId, triggerId);
                logger.warn(strLog);
                return true;
            }

            if ( (taskController.isDtgStillBeingProcessed(xfTask.getTaskflowId(), dtgId, xfTask)
                    || taskController.isDtgCreatorStillBeingProcessed(dtgId))){
                String strSkipReason =
                        String.format("DTG triggered task(selfTaskId=%s, taskflowId=%s, dtgId=%s, triggerId=%s) will be simply ignored because the previous task(s) for the same dtgId has not finished yet",
                                taskId, taskflowId, dtgId, triggerId);
                logger.warn(strSkipReason);
                bContinue = false;
            }
        }
        catch (Exception e)
        {
            String strSkipReason =
                    String.format("DTG triggered task(selfTaskId=%s, taskflowId=%s, dtgId=%s, triggerId=%s) will be ignored because exception: %s",
                            taskId, taskflowId, dtgId, triggerId, e.toString());
            logger.warn(strSkipReason);
            bContinue = false;
        }

        return bContinue;
    }

    public SubmitTaskResult generateAndSubmitTask(String triggerId, String dtgId, boolean isFinal) {
        XFTask xftaskobj = generateXFTask(triggerId, dtgId, isFinal);

        if (xftaskobj == null) {
            return SubmitTaskResult.FAILED;
        }

        boolean bContinue = this.checkIfTriggeredTaskShouldContinue(xftaskobj);
        if (!bContinue)
        {
            return SubmitTaskResult.FAILED;
        }

        int currTaskCount = this.taskInMemoryRepository.getXFTaskCount();
        // if there are already too many task in memory, we should save the new task in DB and not in memory.
        // the task saved into DB will be picked up later by a quartz job to execute it if at that time there are enough resources, etc.
        if (currTaskCount >= commonUtil.maxTaskCountLimitInConfig )
        {
            String strLog =
                    String.format("TaskEngine on %s received new triggered task %s but its task count %d reached limit %d, and thus will be put on the waiting list in DB",
                            commonUtil.localHostName, xftaskobj.getId(), currTaskCount, commonUtil.maxTaskCountLimitInConfig);
            logger.warn(strLog);

            boolean bDBOK = false;
            boolean bTaskcountBelowLimit = false;
            while (true)
            {
                try {
                    taskRepository.save(xftaskobj);

                    bDBOK = true;
                    break;
                }
                catch(Exception e)
                {
                    strLog =
                            String.format("TaskEngine on %s received new triggered task %s but its task count %d reached limit %d, try to save task and taskflow into DB and failed, will retry",
                                    commonUtil.localHostName, xftaskobj.getId(), currTaskCount, commonUtil.maxTaskCountLimitInConfig);
                    logger.warn(strLog, e);
                }

                currTaskCount = this.taskInMemoryRepository.getXFTaskCount();
                bTaskcountBelowLimit = (currTaskCount < commonUtil.maxTaskCountLimitInConfig);
                if (bTaskcountBelowLimit == true)
                {
                    break;
                }

                int sleepTime = 5 * 1000;
                try {

                    Thread.sleep(sleepTime);
                } catch (Exception se) {
                    logger.warn("Exception happened while sleeping " + sleepTime + " during currTaskCount >= this.maxTaskCountLimitInConfig " , se);
                }
            }// end of while

        } // end of if


        // when we get here out of the while loop, we either saved task and taskflow intoDB or the current task count is below limit
        currTaskCount = this.taskInMemoryRepository.getXFTaskCount();
        // if it is still above limit we know that we saved into DB and we just return OK
        if (currTaskCount >= commonUtil.maxTaskCountLimitInConfig )
        {
            return SubmitTaskResult.OK;
        }

        taskController.submitTriggeredTask(xftaskobj);
        return SubmitTaskResult.OK;
    }
}
