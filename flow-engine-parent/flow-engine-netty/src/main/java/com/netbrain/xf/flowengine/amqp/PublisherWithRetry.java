package com.netbrain.xf.flowengine.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PublisherWithRetry {
    private static Logger logger = LogManager.getLogger(PublisherWithRetry.class.getSimpleName());

    @Autowired
    AMQPClient amqpClient;

    /**
     * start with 100 ms
     */
    private long initialInterval = 100L;

    /**
     * (N+1)th = Nth * multipler
     */
    private int multiplier = 2;

    /**
     * Max 30 seconds
     */
    private long maxInterval = 30000L;

    private long waitForConfirmsOrDieIntervalInSeconds = 30;
    private Channel pubChannel ;
    private boolean pubChannelIsConfirmSelectMode;
    public PublisherWithRetry() {

    }
    public long getInitialInterval() {
        return initialInterval;
    }

    public void setInitialInterval(long initialInterval) {
        this.initialInterval = initialInterval;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }

    public long getMaxInterval() {
        return maxInterval;
    }

    public void setMaxInterval(long maxInterval) {
        this.maxInterval = maxInterval;
    }

    private boolean createConfirmSelectAmqpChannel()
    {
        Connection mqConnection = amqpClient.getMqConnection();
        if (mqConnection == null || mqConnection.isOpen() == false)
        {
            logger.error("Failed to get a valid AMQP connection.");
            return false;
        }

        if (this.pubChannel == null || this.pubChannel.isOpen() == false )
        {
            try {
                this.pubChannel = mqConnection.createChannel();
                this.pubChannel.confirmSelect();
            } catch (IOException e) {
                logger.error("Failed to create ConfirmSelect mode channels to messaging server", e);
            }
        }

        return true;
    }

    // This is a synchronized method, based on Rabbitmq API recommendation: Channel#basicPublish must be invoked in a critical section
    // https://www.rabbitmq.com/api-guide.html
    private synchronized boolean publishMessage(String exchange, String routingKey, Object replyProps,String msgBody)
    {
        createConfirmSelectAmqpChannel();
        try {
            pubChannel.waitForConfirmsOrDie(waitForConfirmsOrDieIntervalInSeconds * 1000);
        } catch (Exception e) {
            logger.warn("Failed to publish message because waitForConfirmsOrDie throw exception", e);
            return false;
        }
        try {
            pubChannel.basicPublish(exchange, routingKey, (AMQP.BasicProperties)replyProps, msgBody.getBytes());
            return true;
        } catch (Exception e) {
            logger.warn("Failed to publish message", e);
            return false;
        }
    }

    /**
     * This is not a guaranteed publish action. The user of this method needs to guarantee message
     * publishing on application level.
     *
     * @param exchange
     * @param routingKey
     * @param replyProps
     * @param content
     */
    public void publishWithRetry (String exchange,
                                  String routingKey,
                                  Object replyProps,
                                  String content)
    {
        long totalSpan = 0;
        long currentInterval = initialInterval;
        boolean sentOk = false;
        do {
            sentOk = publishMessage(exchange, routingKey, replyProps, content);
            if (sentOk) {
                break; // no need to sleep if sent
            } else {
                logger.info("Failed to send, wait for {} milliseconds", currentInterval);
            }

            try {
                Thread.sleep(currentInterval);
            } catch (InterruptedException e) {
                logger.error("Exception while waiting for retry", e);
            }

            totalSpan += currentInterval;
            currentInterval *= multiplier;
        } while (!sentOk && totalSpan < maxInterval);

        logger.debug("Done publish message, final result is {}", sentOk);
    }
}
