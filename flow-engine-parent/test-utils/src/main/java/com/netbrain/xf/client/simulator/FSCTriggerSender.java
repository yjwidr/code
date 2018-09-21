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
import java.util.UUID;

/**
 * Send a trigger event on behalf of Front Server Controller
 * See http://confluence.netbraintech.com/confluence/x/g6S1Aw
 */
@SpringBootApplication
public class FSCTriggerSender implements CommandLineRunner{
    private static Logger logger = LogManager.getLogger(FSCTriggerSender.class.getSimpleName());

    private static final String TASK_EXCHANGE_NAME = "fsc_trigger_exchange";
    private static final String TRIGGER_ROUTING_KEY = "notice";

    @Autowired
    AMQPConnectionFactory amqpConnectionFactory;

    private void publishMessage(Channel channel, String correlation, Map<String, Object> headers) {
        logger.info("Sending message to queue {}", headers.toString());
        PublishRetry messagePublisher = new PublishRetry(200L, 2, 60000L);
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                .Builder()
                .correlationId(correlation)
                .replyTo("fsc_triggerreply_queue")
                .deliveryMode(2)
                .priority(0)
                .timestamp(new Date())
                .headers(headers)
                .build();

        messagePublisher.publishWithRetry(channel,
                TASK_EXCHANGE_NAME,
                TRIGGER_ROUTING_KEY,
                replyProps,
                "",
                false);
    }

    @Override
    public void run(String... args) throws Exception {
        Connection connection = amqpConnectionFactory.getMqConnection();
        if (connection != null) {
            Channel channel = connection.createChannel();
            
            Map<String, Object> headers = new HashMap<>();
            headers.put("id", UUID.randomUUID().toString());
            headers.put("taskgroupid", UUID.randomUUID().toString());
            headers.put("type", "trigger");
            headers.put("is_final_trigger", false);
            headers.put("timestamp", "2015-08-31T11:20:00.000Z");

            publishMessage(channel, "", headers);
            channel.close();
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(FSCTriggerSender.class, args);
    }
}
