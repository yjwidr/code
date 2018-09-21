package com.netbrain.xf.flowengine.gateway;

import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.utility.HASupport;
import com.netbrain.xf.xfcommon.XFCommon;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskStatusMessageHandler {
    private static Logger logger = LogManager.getLogger(TaskStatusMessageHandler.class.getSimpleName());

    @Autowired
    private AMQPClient amqpClient;

    @Autowired
    private TaskController taskController;

    @Autowired
    private HASupport haSupport;

    @Autowired
    private DataCenterSwitching dcSwitching;

    // For testing purpose
    protected void setTaskController(TaskController taskController) {
        this.taskController = taskController;
    }

    public void handleMessage(String queueName,
                              Channel channel,
                              String consumerTag,
                              Envelope envelope,
                              AMQP.BasicProperties properties,
                              byte[] body) {
        logger.info("Received a task status request on " + queueName + "" + ", redeliver? " + envelope.isRedeliver());
        try {
            // TODO: consider moving this ack to the bottom of this function
            // This message should be acknowledged in inactive data center
            channel.basicAck(envelope.getDeliveryTag(), false);

            if (!haSupport.isActive() || !dcSwitching.isActiveDC()) {
                logger.debug("Noop in standby mode or inactive DC.");
                return;
            }

            int minorVersion =  amqpClient.extractGenericHeader(properties.getHeaders(), XFCommon.NBMSG_MINOR_VERSION_KEY, 0, false);
            String rcvdMessageType = "";
            if (minorVersion < XFCommon.NBMSG_MINOR_VERSION_VALUE)
            {
                logger.warn("Received task message {}, message minor versin mismatch, flowengine expectes {}, but received minor version {}, please consider upgrade workder server!",rcvdMessageType,  XFCommon.NBMSG_MINOR_VERSION_VALUE, minorVersion);
                rcvdMessageType = amqpClient.extractStringHeader(properties.getHeaders(), AMQPTaskGateway.TASK_TYPE_HEADER_KEY, "", true);
            }
            else
            {
                rcvdMessageType = properties.getType();
            }

            Map<String, Object> headers = properties.getHeaders();
            if (headers != null) {
                String taskId = amqpClient.extractStringHeader(properties.getHeaders(), XFCommon.MSG_KEY_SELF_TASK_ID, "", true);
                String rootTaskId = amqpClient.extractStringHeader(properties.getHeaders(), XFCommon.root_task_id, "", true);
                if (AMQPTaskGateway.TASK_TYPE_TASK_BEGIN.equals(rcvdMessageType)) {
                    int xfagentProcessId = amqpClient.extractGenericHeader(properties.getHeaders(), XFCommon.XFAGENT_PROCESS_ID, -1, false);
                    String xfagentServerName = amqpClient.extractStringHeader(properties.getHeaders(), XFCommon.XFAGENT_SERVER_NAME, null, false);
                    logger.info("Received task status message {} for selfTaskId={} rootTaskId={} ",  rcvdMessageType, taskId, rootTaskId );
                    taskController.processTaskBeginMessage(taskId, xfagentProcessId, xfagentServerName, properties);

                } else if (AMQPTaskGateway.TASK_TYPE_TASK_END.equals(rcvdMessageType)) {
                    logger.info("Received task status message {} for selfTaskId={} rootTaskId={} ",  rcvdMessageType, taskId, rootTaskId );
                    int taskCompleteStatus = amqpClient.extractGenericHeader(headers, XFCommon.complete_status, XFCommon.TASKSTATUS_CompletedNormally, true);
                    taskController.processTaskEndMessage(taskId, taskCompleteStatus);
                }
                else {
                    logger.warn("Unknown task message content type {}, message will be ignored! ", rcvdMessageType);
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
}
