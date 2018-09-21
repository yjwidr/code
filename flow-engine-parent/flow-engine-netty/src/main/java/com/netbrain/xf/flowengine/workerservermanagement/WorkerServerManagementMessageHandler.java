package com.netbrain.xf.flowengine.workerservermanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.netbrain.xf.flowengine.gateway.AMQPTaskGateway;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.flowengine.utility.CommonUtil;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.xfcommon.XFCommon;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

@Component
public class WorkerServerManagementMessageHandler {
    private static Logger logger = LogManager.getLogger(WorkerServerManagementMessageHandler.class.getSimpleName());

    @Autowired
    private AMQPClient amqpClient;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private DataCenterSwitching dcSwitching;

    @Autowired
    com.netbrain.xf.flowengine.daoinmemory.XFAgentInMemoryRepository xfAgentInMemoryRepository;

    public void handleMessage(String queueName,
                              Channel channel,
                              String consumerTag,
                              Envelope envelope,
                              AMQP.BasicProperties properties,
                              byte[] body) {

        String rcvdMsgType = properties.getType();
        if (StringUtils.isEmpty(rcvdMsgType))
        {
            rcvdMsgType = "";
            logger.warn("Received a worker server management message on " + queueName + ", it is missing AMQP.BasicProperties.getType(), thus will be ignored!");
            return;
        }
        try {
            channel.basicAck(envelope.getDeliveryTag(), false);

            if(!dcSwitching.isActiveDC()){
                logger.debug("Noop in inactive DC.");
                return;
            }

            Map<String, Object> headers = properties.getHeaders();
            if (headers == null)
            {
                logger.warn("Missing message headers for a message received on queue " + queueName);
            }
            else if (rcvdMsgType.equals(XFCommon.XFMessageTypes.MSGTYPE_XFAgent_HeartBeat))
            {
                int xfagentPID = amqpClient.extractGenericHeader(headers, XFCommon.XFFieldTypes.FieldTYPE_XFAgentProcessId, -1, true);
                String xfagentHostName = amqpClient.extractStringHeader(headers, XFCommon.XFFieldTypes.FieldTYPE_WorkerServerHostName, "", true);
                String strLog = "";
                String strBody = new String(body);
                if (StringUtils.isEmpty(strBody))
                {
                    strLog = String.format("Received a worker server management message of msgType %s, xfagentPID = %d from server %s, but message body is empty or null, message will be ignored!", rcvdMsgType, xfagentPID, xfagentHostName);
                    logger.warn(strLog);
                }
                else
                {
                    strLog = String.format("Received a worker server management message of msgType %s, xfagentPID = %d from server %s, msgBody is \n %s",
                            rcvdMsgType, xfagentPID, xfagentHostName, strBody);
                    logger.debug(strLog);
                    XFAgentHeartBeatMessage retHBMsg = CommonUtil.convertJsonStr2XFAgentHeartBeatMessage(strBody);
                    //XFAgent retXFAgent = XFAgent.convertJsonStr2XFAgentObject(strBody);
                    if (retHBMsg != null && retHBMsg.xfAgentInfo != null &&  StringUtils.isEmpty(retHBMsg.xfAgentInfo.getId()) == false )
                    {
                        strLog = String.format("Successfully deserialize received worker server management message of msgType %s, xfagentPID = %d from server %s, msgBody is \n %s",
                                rcvdMsgType, xfagentPID, xfagentHostName, strBody);
                        logger.debug(strLog);

                        // Heartbeat things to do #1 - update XFAgentInMemoryRepository
                        XFAgent rcvdXFAgent = retHBMsg.xfAgentInfo;
                        XFAgent agengInfoInMem = this.xfAgentInMemoryRepository.GetOneXFAgent(rcvdXFAgent.getServerName());
                        commonUtil.ProcessNewlyReceivedXFAgentInformation(agengInfoInMem, rcvdXFAgent, XFCommon.XFAgentInformationFrom.FROM_RABBITMQ_HB_MESSAGE);

                        // Heartbeat things to do #2 - update XFTaskInMemoryRepository
                        commonUtil.ProcessNewlyReceivedXFTaskSummaryInfo(retHBMsg.selfTaskId2XFTaskSummaryDict);
                    }
                    else
                    {
                        strLog = String.format("Failed to deserialize received worker server management message of msgType %s, xfagentPID = %d from server %s, msgBody is \n %s",
                                rcvdMsgType, xfagentPID, xfagentHostName, strBody);
                        logger.warn(strLog);
                    }
                }
            }
            else if (rcvdMsgType.equals(XFCommon.XFMessageTypes.MSGTYPE_XFAgent_Crashed)) {
                int xfagentPID = amqpClient.extractGenericHeader(headers, XFCommon.XFFieldTypes.FieldTYPE_XFAgentProcessId, -1, true);
                String xfagentHostName = amqpClient.extractStringHeader(headers, XFCommon.XFFieldTypes.FieldTYPE_WorkerServerHostName, "", true);

                String strLog = String.format("Received a worker server management message of msgType %s, xfagentPID = %d from server %s", rcvdMsgType, xfagentPID, xfagentHostName);
                logger.warn(strLog);
            }
            else if (rcvdMsgType.equals(XFCommon.XFMessageTypes.MSGTYPE_XFAgent_ShuttingDown))
            {
                int xfagentPID = amqpClient.extractGenericHeader(headers, XFCommon.XFFieldTypes.FieldTYPE_XFAgentProcessId, -1, true);
                String xfagentHostName = amqpClient.extractStringHeader(headers, XFCommon.XFFieldTypes.FieldTYPE_WorkerServerHostName, "", true);
                String xfagentRunningInstanceId = amqpClient.extractStringHeader(headers, XFCommon.STR_xfAgentRunningInstanceId, "", true);
                String xfAgentRunningInstanceIdInMemory = "";

                String strReason = String.format("---------- Received a worker server management message of msgType %s, xfagentPID = %d from server %s", rcvdMsgType, xfagentPID, xfagentHostName);
                logger.warn(strReason);
                XFAgentMetadata metadataInMem = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(xfagentHostName);
                XFAgent agentInfoInMem = this.xfAgentInMemoryRepository.GetOneXFAgent(xfagentHostName);
                if(agentInfoInMem != null){
                    xfAgentRunningInstanceIdInMemory = agentInfoInMem.getXfAgentRunningInstanceId();
                }
                if (metadataInMem != null && xfAgentRunningInstanceIdInMemory.equals(xfagentRunningInstanceId)) {
                    commonUtil.handleXFAgentCrash(xfagentHostName, xfagentRunningInstanceId, xfagentPID, metadataInMem, strReason, XFCommon.XFAgentBlacklistedReasonCode.REASON_DUE_TO_XFAGENT_RESTARTED);
                }
            }
            else {
                logger.warn("Unknown message type " + rcvdMsgType);
            }

        } catch (IllegalArgumentException iae) {
            logger.warn("Bad message received on queue " + queueName, iae);
        } catch (Exception e) {
            logger.warn("Failed to process management message received on queue " + queueName, e);
        }
    }

}
