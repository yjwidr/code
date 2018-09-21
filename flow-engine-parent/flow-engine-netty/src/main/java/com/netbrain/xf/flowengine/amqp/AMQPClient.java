package com.netbrain.xf.flowengine.amqp;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.netbrain.xf.flowengine.config.RabbitmqConfig;
import com.netbrain.xf.flowengine.utility.CommonUtil;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.LongString;

import io.netty.handler.ssl.JdkSslClientContext;
@Component
public class AMQPClient {
    private static Logger logger = LogManager.getLogger(AMQPClient.class.getSimpleName());

    @Autowired
    RabbitmqConfig rabbitmqConfig;

    private ConnectionFactory mqConnectionFactory = new ConnectionFactory();

    public void setMqConnectionFactory(ConnectionFactory mqConnectionFactory) {
        this.mqConnectionFactory = mqConnectionFactory;
    }

    private boolean isLeader = false;
    private Connection mqConnection;

    /**
     * A blocking call to create a RabbitMQ connection.
     * It will wait until a good connection is established and the AMQPClient has leader token.
     * @return the connection
     */
    public Connection getMqConnection() {
        while (!isLeader) {
            logger.debug("not a leader");
            try {
                Thread.sleep(rabbitmqConfig.getReconnectInterval());
            } catch (InterruptedException e1) {
                logger.error("Failed to wait for leader token, will retry");
            }
        }

        return getMqConnectionWithoutLeaderChecking();
    }

    /**
     * A blocking call to create a RabbitMQ connection.
     * It will wait until a good connection is established.
     * @return the connection
     */
    public Connection getMqConnectionWithoutLeaderChecking() {
        while (mqConnection == null || !mqConnection.isOpen()) {
            try {
                String[] addrs = rabbitmqConfig.getAddrs().split(" *, *");
                Address[] res = new Address[addrs.length];
                for (int i = 0; i < addrs.length; i++) {
                    res[i] = new Address(addrs[i], rabbitmqConfig.getPort());
                }

                mqConnection = mqConnectionFactory.newConnection(res);
            } catch (Exception e) {
                logger.error("Failed to create RabbitMQ connections, will retry soon. Error: " + e.getMessage());
                try {
                    Thread.sleep(rabbitmqConfig.getReconnectInterval());
                } catch (InterruptedException e1) {
                    logger.error("Failed to wait for RabbitMQ connection");
                }
            }
        }
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
        mqConnectionFactory.setAutomaticRecoveryEnabled(true); // auto-recover connections and channels
        mqConnectionFactory.setTopologyRecoveryEnabled(true); // auto-recover exchanges, queues and consumers

        if (rabbitmqConfig.isSsl()) {
            try
            {
                if(rabbitmqConfig.getCert_verification().equalsIgnoreCase("verify_ca_no_limit")) {
                    File certFile = CommonUtil.getCertFile();
                    if(!ObjectUtils.isEmpty(certFile)) {
                        JdkSslClientContext jdkSslClientContext = new JdkSslClientContext(certFile);
                        mqConnectionFactory.useSslProtocol(jdkSslClientContext.context());
                    }else {
                        JdkSslClientContext jdkSslClientContext = new JdkSslClientContext(CommonUtil.getTrustManager());
                        mqConnectionFactory.useSslProtocol(jdkSslClientContext.context());
                    }
                }else if(rabbitmqConfig.getCert_verification().equalsIgnoreCase("none")) {
                    mqConnectionFactory.useSslProtocol();
                }
            }
            catch( SSLException e) {
                logger.error("Failed to create RabbitMQ connection with SSL", e);
            }
        }
    }

    public void setLeaderStatus(boolean isLeader) {
        this.isLeader = isLeader;
    }

    public String getUniqueConsumerTag(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String isRabbitCaught() {
        return (mqConnection != null && mqConnection.isOpen()) ? "yes" : "no";
    }

    public <T> T extractGenericHeader(Map<String, Object> headers,
                                       String headerName,
                                       T defaultVal,
                                       boolean throwExceptionWhenNull) {
        if (headers.get(headerName) == null) {
            if (throwExceptionWhenNull) {
                throw new IllegalArgumentException("Missing header " + headerName + " in message headers");
            } else {
                return defaultVal;
            }
        }

        return (T)headers.get(headerName);
    }

    public String extractStringHeader(Map<String, Object> headers,
                                       String headerName,
                                       String defaultVal,
                                       boolean throwExceptionWhenNull) {
        if (headers.get(headerName) == null) {
            if (throwExceptionWhenNull) {
                throw new IllegalArgumentException("Missing header " + headerName + " in message headers");
            } else {
                return defaultVal;
            }
        }

        if (headers.get(headerName) instanceof LongString) {
            return ((LongString) headers.get(headerName)).toString();
        } else {
            return (String)headers.get(headerName);
        }
    }
}
