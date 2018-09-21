package com.netbrain.xf.flowengine.utility;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.netbrain.xf.flowengine.config.FlowEngineConfig;
import com.netbrain.xf.flowengine.metric.Metrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.AMQP.Queue;
import org.springframework.util.StringUtils;

@Component
public class HASupport {
    private static Logger logger = LogManager.getLogger(HASupport.class.getSimpleName());

    @Autowired
    AMQPClient amqpClient;

    private Channel channel;
    
    private String leader;

    @Autowired
    private FlowEngineConfig flowEngineConfig;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private Metrics metrics;

    @Autowired
    private DataCenterSwitching dcSwitching;

    public boolean isActive() {
        if (flowEngineConfig.isHAEnabled() && (StringUtils.isEmpty(leader) || !leader.equals("leader"))) {
            return false;
        }
        return true;
    }

    protected String getLeader() {
        return leader;
    }

    protected void setLeader(String leader) {
        this.leader = leader;
    }

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        tryAcquiringLeaderRole();
    }

    private void giveUpLeaderToken() {
        this.leader = "";
    }

    private void handleStepDownFromLeader() {
        if (flowEngineConfig.isHAEnabled()) {
            if (isActive()) {
                logger.fatal("STEPPING DOWN FROM LEADER ROLE, EXITING...");
                int exitCode = SpringApplication.exit(appContext, () -> 64);
                System.exit(exitCode);
            } else {
                giveUpLeaderToken();
            }
        }
    }

    // synchronized: avoid concurrency when consuming and acking messages, see https://www.rabbitmq.com/api-guide.html
    public synchronized void tryAcquiringLeaderRole() {
        boolean initSuccessfully = false;
        do {
            try {
                Map<String, Object> args = new HashMap<String, Object>();
                args.put("x-max-length", 1);
                String requestQueueName = "nb_flowengine_leader";
                Connection mqConnection = amqpClient.getMqConnectionWithoutLeaderChecking();
                channel = mqConnection.createChannel();
                channel.basicQos(1);
                Queue.DeclareOk result = channel.queueDeclare(requestQueueName, true, false, false, args);
                if (result.getConsumerCount() == 0 && result.getMessageCount() == 0) {
                    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                            .contentType("application/json")
                            .deliveryMode(2)
                            .build();
                    channel.basicPublish("", requestQueueName, props, "leader".getBytes());
                }
                channel.basicConsume(requestQueueName, false, new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                               byte[] body) throws IOException {
                        String leader = new String(body);
                        logger.warn("RUNNING AS LEADER");
                        setLeader(leader);
                        amqpClient.setLeaderStatus(true);
                        logger.debug("leader=" + leader);
                        metrics.addStepupCount(1);
                    }

                    @Override
                    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                        logger.warn("disconnect from rabbitmq and losing leader role");
                        handleStepDownFromLeader();
                        metrics.addStepdownCount(1);
                    }
                });
                initSuccessfully = true;
            } catch (IOException e) {
                // ENG-40964, sometimes a connection can be established but QueueDeclare fails. we need to keep retrying
                // until consumer is setup.
                logger.error("Failed to setup consumer to messaging server, will try again soon", e);
                initSuccessfully = false;
                try {
                    Thread.sleep(10 * 1000);
                } catch (Exception se) {
                    logger.warn("Exception happened while waiting", se);
                }
            }
        } while (!initSuccessfully);
    }
}
