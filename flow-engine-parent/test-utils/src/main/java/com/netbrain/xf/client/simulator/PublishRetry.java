package com.netbrain.xf.client.simulator;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PublishRetry {
    private static Logger logger = LogManager.getLogger(PublishRetry.class.getSimpleName());

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

    public PublishRetry(long initialInterval, int multiplier, long maxInterval) {
        this.initialInterval = initialInterval;
        this.multiplier = multiplier;
        this.maxInterval = maxInterval;
    }

    private boolean publishMessage(Channel channel,
                                   String exchange,
                                   String routingKey,
                                   AMQP.BasicProperties replyProps,
                                   String content,
                                   boolean usePublisherConfirms) {
        try {
            channel.basicPublish(exchange, routingKey, replyProps, content.getBytes());

            if (usePublisherConfirms) {
                channel.waitForConfirmsOrDie(initialInterval);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Failed to publish message", e);
            return false;
        }
    }

    public void publishWithRetry (Channel channel,
                                  String exchange,
                                  String routingKey,
                                  AMQP.BasicProperties replyProps,
                                  String content,
                                  boolean usePublisherConfirms){
        long totalSpan = 0;
        long currentInterval = initialInterval;
        boolean sentOk = false;
        do {
            sentOk = publishMessage(channel, exchange, routingKey, replyProps, content, usePublisherConfirms);
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
