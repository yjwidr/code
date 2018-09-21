package com.netbrain.xf.flowengine.utility;

import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.netbrain.xf.flowengine.amqp.PublisherWithRetry;
import com.netbrain.xf.xfcommon.XFCommon;
import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
public class DataCenterSwitching {
    private static Logger logger = LogManager.getLogger(DataCenterSwitching.class.getSimpleName());

    // Multi DC Support: status of current dc
    public static final int MULTIDC_CURRENTDC_NOTSUPPORTED = -1;
    public static final int MULTIDC_CURRENTDC_INACTIVE = 0;
    public static final int MULTIDC_CURRENTDC_ACTIVE = 1;
    public static final int MULTIDC_CURRENTDC_DEACTIVATING = 2;
    public static final int MULTIDC_CURRENTDC_ACTIVATING = 3;

    // Elements of RabbitMQ
    public static final String DCSWITCHING_EXCHANGE = "nb_dcswitching_exchange";
    public static final String DCSWITCHING_QUEUE_CMD = "nb_flowengine_dcswitching_cmd_queue";
    public static final String DCSWITCHING_QUEUE_NOTIFY_XF = "nb_flowengine_%s_dcswitching_notify_queue";
    public static final String DCSWITCHING_ROUTINGKEY_NOTIFY_XF = "nb_flowengine_dcswitching_notify_queue";
    public static final String DCSWITCHING_ROUTINGKEY_NOTIFY_RM = "nb_resourcemanager_dcswitching_notify_queue";
    public static final String DCSWITCHING_HEADER_DC_STATUS = "dc_status";
    public static final String DCSWITCHING_HEADER_DB_LIST = "db_list";
    //private static final String DCSWITCHING_CMD_UPDATE_INVENTORY = "resource_manager_command_update_inventory";
    //private static final String DCSWITCHING_CMD_REDISTRIBUTE_MACHINES_RES = "redistribute_machines_resources";
    public static final String DCSWITCHING_CMD_DISABLE_SYS = "disable_system";
    public static final String DCSWITCHING_CMD_ENABLE_SYS = "enable_system";
    public static final String DCSWITCHING_CMD_SYS_STATUS = "system_status";

    @Autowired
    AMQPClient amqpClient;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private HASupport haSupport;

    @Autowired
    private PublisherWithRetry publisher;

    // A communication channel to RabbitMQ
    private Channel channel;

    // private String cmdQueueConsumerTag;

    @Value("${multidc.currentdc.status}")
    private int currentDCStatus;
    public int getCurrentDCStatus(){
        return currentDCStatus;
    }

    @Value("${multidc.currendc.dblist}")
    private String currentDCDBList;

    @Value("${flowengine.config.filepath}")
    private String configFilepath;

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        initDCSwitchingMsgPipe();
    }

    // Check if current data center is active
    public boolean isActiveDC(){
        if(this.currentDCStatus == MULTIDC_CURRENTDC_ACTIVE || this.currentDCStatus == MULTIDC_CURRENTDC_NOTSUPPORTED){
            return true;
        }
        return false;
    }

    // Initialize DC switching message pipe
    private synchronized void initDCSwitchingMsgPipe(){
        boolean initSuccessfully = false;
        do {
            try {
                // Create and initialize a new rabbitMQ channel
                Connection mqConnection = amqpClient.getMqConnectionWithoutLeaderChecking();
                channel = mqConnection.createChannel();
                channel.basicQos(1);

                // Make sure rabbitMQ exchange "nb_dcswitching_exchange" is created and initialized
                channel.exchangeDeclare(DCSWITCHING_EXCHANGE, XFCommon.RabbitMqString.EXCHANGE_TYPE_DIRECT, true);

                /* Make sure rabbitMQ queue "nb_flowengine_dcswitching_cmd_queue"
                is created and bound to exchange "nb_dcswitching_exchange" */
                AMQP.Queue.DeclareOk dcSwitchingCmdQueue = channel.queueDeclare(DCSWITCHING_QUEUE_CMD,
                        true, false, false, null);
                channel.queueBind(DCSWITCHING_QUEUE_CMD, DCSWITCHING_EXCHANGE, DCSWITCHING_QUEUE_CMD);
                // Only one XF leader instance has consumer for queue "nb_flowengine_dcswitching_cmd_queue"
                if(haSupport.isActive() && dcSwitchingCmdQueue.getConsumerCount() == 0){
                    createConsumerForCmdQueue();
                }

                /* Make sure rabbitMQ queue "nb_flowengine_%s_dcswitching_notify_queue"
                is created and bound to exchange "nb_dcswitching_exchange" */
                String dcSwitchingNotifyXFQueue = String.format(DCSWITCHING_QUEUE_NOTIFY_XF,
                        commonUtil.localHostName);
                channel.queueDelete(dcSwitchingNotifyXFQueue);
                channel.queueDeclare(dcSwitchingNotifyXFQueue, true, false, false, null);
                channel.queueBind(dcSwitchingNotifyXFQueue, DCSWITCHING_EXCHANGE, DCSWITCHING_ROUTINGKEY_NOTIFY_XF);
                createConsumerForNotifyXFQueue();

                initSuccessfully = true;
            } catch (IOException e) {
                logger.error("Failed to initialize multi data center switching message pipe, will try again soon.", e);
                initSuccessfully = false;
                try {
                    Thread.sleep(10 * 1000);
                } catch (Exception se) {
                    logger.warn("Exception happened while waiting to create multi data center switching message pipe", se);
                }
            }
        } while (!initSuccessfully);
    }

    // Create consumer for queue "nb_flowengine_dcswitching_cmd_queue"
    private void createConsumerForCmdQueue(){
        boolean initSuccessfully = false;
        do {
            try {
                channel.basicConsume(DCSWITCHING_QUEUE_CMD, false, new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                               byte[] body) throws IOException {
                        try {
                            // cmdQueueConsumerTag = consumerTag;
                            String rcvdMessageType = properties.getType();
                            String reply2Destination = properties.getReplyTo();

                            if (!rcvdMessageType.isEmpty() && !reply2Destination.isEmpty()) {
                                switch (rcvdMessageType) {
                                    case DCSWITCHING_CMD_SYS_STATUS: {
                                        handleCheckSysStatus(rcvdMessageType, reply2Destination);
                                        break;
                                    }
                                    case DCSWITCHING_CMD_DISABLE_SYS: case DCSWITCHING_CMD_ENABLE_SYS: {
                                        handleSysSwitchMsg(rcvdMessageType, reply2Destination);
                                        break;
                                    }
                                    default: {
                                        logger.warn(String.format("Received a message with unknown type %s on queue %s.",
                                                rcvdMessageType, DCSWITCHING_QUEUE_CMD));
                                        break;
                                    }
                                }
                            } else {
                                logger.warn(String.format("Missing message type & reply to for a message received on queue %s.", DCSWITCHING_QUEUE_CMD));
                            }
                        } catch (Exception e) {
                            logger.error(String.format("Failed to process message received on queue %s.",
                                    DCSWITCHING_QUEUE_CMD), e);
                        } finally {
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        }
                    }
                });
                initSuccessfully = true;
            } catch (Exception e) {
                logger.error(String.format("Failed to create consumer for queue %s, will try again soon.",
                        DCSWITCHING_QUEUE_CMD), e);
                initSuccessfully = false;
                try {
                    Thread.sleep(10 * 1000);
                } catch (Exception se) {
                    logger.warn(String.format("Exception happened while waiting to create consumer for queue %s, will try again soon.",
                            DCSWITCHING_QUEUE_CMD), e);
                }
            }
        } while (!initSuccessfully);
    }

    // Handle check system status request
    private void handleCheckSysStatus(String rcvdMessageType, String reply2Destination){
        try {
            logger.info(String.format("Received a message with type %s on queue %s.",
                    rcvdMessageType, DCSWITCHING_QUEUE_CMD));

            Map<String, Object> headers = new HashMap<>();
            headers.put(DCSWITCHING_HEADER_DC_STATUS, currentDCStatus);
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .headers(headers)
                    .type(rcvdMessageType)
                    .contentType(XFCommon.RabbitMqString.MSG_TYPE_JSON)
                    .build();
            publisher.publishWithRetry(DCSWITCHING_EXCHANGE, reply2Destination, props, "");
        }
        catch (Exception e){
            logger.error(String.format("Failed to process message received on queue %s with type %s.",
                    DCSWITCHING_QUEUE_CMD, rcvdMessageType), e);
        }
    }

    // Handle DC switching request
    private void handleSysSwitchMsg(String rcvdMessageType, String reply2Destination){
        try {
            logger.info(String.format("Received a message with type %s on queue %s.",
                    rcvdMessageType, DCSWITCHING_QUEUE_CMD));
            // send reply message to web api server
            Map<String, Object> headers = new HashMap<>();
            AMQP.BasicProperties props2APIServer = new AMQP.BasicProperties
                    .Builder()
                    .headers(headers)
                    .type(rcvdMessageType)
                    .contentType(XFCommon.RabbitMqString.MSG_TYPE_JSON)
                    .build();
            publisher.publishWithRetry(DCSWITCHING_EXCHANGE, reply2Destination, props2APIServer, "");

            // Broadcast to all XF instances
            AMQP.BasicProperties props2XF = new AMQP.BasicProperties
                    .Builder()
                    .headers(headers)
                    .type(rcvdMessageType)
                    .contentType(XFCommon.RabbitMqString.MSG_TYPE_JSON)
                    .build();
            publisher.publishWithRetry(DCSWITCHING_EXCHANGE, DCSWITCHING_ROUTINGKEY_NOTIFY_XF, props2XF, "");
        }
        catch (Exception e){
            logger.error(String.format("Failed to process message received on queue %s with type %s.",
                    DCSWITCHING_QUEUE_CMD, rcvdMessageType), e);
        }
    }

    // Create consumer for queue "nb_flowengine_%s_dcswitching_notify_queue"
    private void createConsumerForNotifyXFQueue(){
        boolean initSuccessfully = false;
        String dcSwitchingNotifyXFQueue = String.format(DCSWITCHING_QUEUE_NOTIFY_XF,
                commonUtil.localHostName);
        do {
            try {
                channel.basicConsume(dcSwitchingNotifyXFQueue, false, new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                               byte[] body) throws IOException {
                        try {
                            String rcvdMessageType = properties.getType();
                            if (!rcvdMessageType.isEmpty()) {
                                switch (rcvdMessageType) {
                                    case DCSWITCHING_CMD_DISABLE_SYS: {
                                        switch2Inactive();
                                        break;
                                    }
                                    case DCSWITCHING_CMD_ENABLE_SYS: {
                                        switch2Active();
                                        break;
                                    }
                                    default: {
                                        logger.warn(String.format("Received a message with unknown type %s on queue %s.",
                                                rcvdMessageType, dcSwitchingNotifyXFQueue));
                                        break;
                                    }
                                }
                            } else {
                                logger.warn(String.format("Missing message type for a message received on queue %s.",
                                        dcSwitchingNotifyXFQueue));
                            }
                        } catch (Exception e) {
                            logger.warn(String.format("Failed to process message received on queue %s.",
                                    DCSWITCHING_QUEUE_CMD), e);
                        } finally {
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        }
                    }
                });
                initSuccessfully = true;
            } catch (Exception e) {
                logger.error(String.format("Failed to create consumer for queue %s, will try again soon.",
                        dcSwitchingNotifyXFQueue), e);
                initSuccessfully = false;
                try {
                    Thread.sleep(10 * 1000);
                } catch (Exception se) {
                    logger.warn(String.format("Exception happened while waiting to create consumer for queue %s, will try again soon.",
                            dcSwitchingNotifyXFQueue), e);
                }
            }
        } while (!initSuccessfully);
    }

    // Switch system to inactive status in current data center
    private void switch2Inactive(){
        try {
            if(currentDCStatus != MULTIDC_CURRENTDC_INACTIVE){
                logger.info("Start to deactivate current data center.");
                currentDCStatus = MULTIDC_CURRENTDC_DEACTIVATING;
                if(haSupport.isActive()){
                    logger.info("Executing deactivate action as leader.");
                    NotifyRMDCSwitching(DCSWITCHING_CMD_DISABLE_SYS);
                    // FSC
                    // CANCLE DTG, Analysis Task, Task Flow
                }
                // Over write config file
                boolean writeConfigSuccessfully = false;
                int retryTimes = 3;
                do {
                    retryTimes -= 1;
                    writeConfigSuccessfully = writeConfigFile(MULTIDC_CURRENTDC_INACTIVE);
                }
                while (!writeConfigSuccessfully && retryTimes > 0);

                if(writeConfigSuccessfully){
                    currentDCStatus = MULTIDC_CURRENTDC_INACTIVE;
                    logger.info(String.format(
                            "Current data center has been deactivated successfully. Current status:%s, current dblist:%s.",
                            currentDCStatus, currentDCDBList));
                }
                else {
                    logger.error("Current data center deactivate failed.");
                }
            }
            else {
                logger.warn("Current data center has been deactivated.");
            }
        }
        catch (Exception e){
            logger.error("Exception happened while deactivating current data center.", e);
        }
    }

    // Switch system to active status in current data center
    private void switch2Active(){
        try {
            if(currentDCStatus != MULTIDC_CURRENTDC_ACTIVE){
                logger.info("Start to activate current data center.");
                currentDCStatus = MULTIDC_CURRENTDC_ACTIVATING;
                // Broadcast to RM
                currentDCStatus = MULTIDC_CURRENTDC_ACTIVE;
            }
            else {
                // if dbList not null, write to config file
                logger.warn("Current data center has been activated.");
            }
        }
        catch (Exception e){
            logger.error("Exception happened while activating current data center.", e);
        }
    }

    // Notify RM switching data center status
    private void NotifyRMDCSwitching(String switchingCmd)
    {
        try {
            logger.info(String.format("Notify RM switching data center with command %s.", switchingCmd));

            Map<String, Object> headers = new HashMap<>();
            headers.put(DCSWITCHING_HEADER_DB_LIST, currentDCDBList);
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .headers(headers)
                    .type(switchingCmd)
                    .contentType(XFCommon.RabbitMqString.MSG_TYPE_JSON)
                    .build();
            publisher.publishWithRetry("monitor_exchange", "", props, "");
        }
        catch (Exception e){
            logger.info(String.format("Notify RM switching data center with command %s failed.", switchingCmd));
        }
    }

    // Write new data center status and db list into configuration file
    private boolean writeConfigFile(int newDCStatus){
        try {
            // Load key configuration items in config file
            Properties flowengineProps = commonUtil.loadConfigFile();
            if(flowengineProps.containsKey("multidc.currentdc.status")){
                flowengineProps.setProperty("multidc.currentdc.status", Integer.toString(newDCStatus));
                commonUtil.writeConfigFile(flowengineProps);
                return true;
            }
            else{
                logger.error("Can not load config file correctly, data center switching failed.");
                return false;
            }
        } catch (Exception e) {
            logger.error("Failed to load key configuration items from properties file " + configFilepath, e);
            return false;
        }
    }

   /* public synchronized void retiredConsumerOfCmdQueue(){
        try
        {
            if(cmdQueueConsumerTag != null && !cmdQueueConsumerTag.isEmpty()) {
                channel.basicCancel(cmdQueueConsumerTag);
                logger.info("Retired data center switching cmd queue successfully.");
            }
            else {
                logger.warn("Retired data center switching cmd queue on a backup XF instance.");
            }
        }
        catch (Exception e){
            logger.error("Retired data center switching cmd queue failed.");
        }
    } */

    // Publish rabbitMQ message
   /* public void publishMsg(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body){
        try {
            logger.info(String.format("Publish a message to exchange %s with routing key %s.",
                    exchange, routingKey));
            channel.basicPublish(exchange, routingKey, true, props, body);
        }
        catch (Exception e){
            logger.error(String.format("Failed to publish message to exchange %s with routing key %s.",
                    exchange, routingKey), e);
        }
    } */
}