package com.netbrain.xf.client.simulator;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Send a TASKFLOW_CREATE_REQ command to RabbitMQ on behalf on XFClient.RunJob API
 * See http://confluence.netbraintech.com/confluence/x/hkWbAw about TASKFLOW_CREATE_REQ
 * See http://confluence.netbraintech.com/confluence/x/q4a1Aw about XFClient.RunJob API
 *
 * For detailed message format, see
 * http://confluence.netbraintech.com/confluence/x/_Iy1Aw
 */
@SpringBootApplication
public class XFClientRunJobCommand implements CommandLineRunner{
    private static Logger logger = LogManager.getLogger(XFClientRunJobCommand.class.getName());

    private static final String TASK_EXCHANGE_NAME = "ready_tasks_exchange";
    private static final String TASK_QUEUE_NAME = "prepared_tasks";
    private static final String CONSUMER_TAG = "flow-engine-task-gw";

    @Autowired
    AMQPConnectionFactory amqpConnectionFactory;

    private void publishMessage(Channel channel, String correlation, Map<String, Object> headers) {
        logger.debug("Sending message to control queue {}", headers.toString());
        PublishRetry messagePublisher = new PublishRetry(200L, 2, 60000L);
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(correlation)
                .replyTo("Nothing")
                .deliveryMode(1)
                .priority(170)
                .timestamp(new Date())
                .headers(headers)
                .build();

        messagePublisher.publishWithRetry(channel,
                TASK_EXCHANGE_NAME,
                TASK_QUEUE_NAME,
                replyProps,
                "{\"name\": \"panda\"}",
                false);
    }

    @Override
    public void run(String... args) throws Exception {
        Connection connection = amqpConnectionFactory.getMqConnection();
        if (connection != null) {
            Channel channel = connection.createChannel();
            
            Map<String, Object> headers = new HashMap<>();
            headers.put("user_IPAddress", "::1");
            headers.put("user_name", "admin");
            headers.put("WorkerRestartTimes", -2);
            headers.put("root_task_id", "cb0e8c27-ccc0-4498-a955-b7fb19002f76");
            headers.put("shortDescription", "");
            headers.put("domainId", "fbcdb735-63b3-4736-b890-a53470f693b9");
            headers.put("tenantDbName", "T1");
            headers.put("domainDbName", "D1");
            headers.put("task_message_content_type", "task_message_content_type_task");
            headers.put("jobRunCategory", "RunAsOnDemandJob");
            headers.put("needBroadCallbackToAllApiServer", false);
            headers.put("task_priority", 10);
            headers.put("tenantId", "d5260aaa-5aa4-3bb5-b50e-2fb3203735d4");
            headers.put("task_callback_queue", "RMClientCallback_NB-DTP-284N_DefaultWebSite");
            headers.put("task_job_id", "cb0e8c27-ccc0-4498-a955-b7fb19002f7");
            headers.put("task_type", "TestProxyServer");

            publishMessage(channel, "", headers);
            channel.close();
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(XFClientRunJobCommand.class, args);
    }
}
