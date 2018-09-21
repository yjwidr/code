package com.netbrain.xf.client.simulator;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Component
public class AMQPConnectionFactory {
    private static Logger logger = LogManager.getLogger(AMQPConnectionFactory.class.getSimpleName());

    @Autowired
    XFClientRabbitmqConfig rabbitmqConfig;

    private ConnectionFactory mqConnectionFactory = new ConnectionFactory();

    public void setMqConnectionFactory(ConnectionFactory mqConnectionFactory) {
        this.mqConnectionFactory = mqConnectionFactory;
    }

    private Connection mqConnection;
    public Connection getMqConnection() {
        return mqConnection;
    }

    /**
     * This is called by the Spring container after the bean is created.
     * Currently it does not throw exception even if it fails to connect ot RabbitMQ.
     * @throws Exception
     */
    @PostConstruct
    public void initRMQ() throws Exception {
        mqConnectionFactory.setUsername(rabbitmqConfig.getUser());
        mqConnectionFactory.setPassword(rabbitmqConfig.getPass());
        mqConnectionFactory.setPort(rabbitmqConfig.getPort());
        try {
            mqConnection = mqConnectionFactory.newConnection(Address.parseAddresses(rabbitmqConfig.getAddrs()));
        } catch (IOException e) {
            logger.error("Failed to create RabbitMQ connection", e);

        } catch (TimeoutException e) {
            logger.error("Failed to create RabbitMQ connection", e);
        }
    }
}
