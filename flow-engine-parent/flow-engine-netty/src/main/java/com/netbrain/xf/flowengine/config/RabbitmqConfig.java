package com.netbrain.xf.flowengine.config;

import com.netbrain.xf.flowengine.gateway.AMQPTaskGateway;
import com.netbrain.xf.flowengine.utility.Cryptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Component
@ConfigurationProperties(prefix="rabbitmq")
@Validated
public class RabbitmqConfig {
    private static Logger logger = LogManager.getLogger(RabbitmqConfig.class.getSimpleName());

    private String cert_verification;
    
    private boolean ssl;

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }
    
    public String getCert_verification() {
        return cert_verification;
    }
    
    public void setCert_verification(String cert_verification) {
        this.cert_verification = cert_verification;
    }

    @NotNull
    private String addrs;
    public String getAddrs() {
        return addrs;
    }

    public void setAddrs(String addrs) {
        this.addrs = addrs;
    }

    @Min(1)
    @Max(65536)
    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @NotNull
    private String user;

    public String getUser() {
        // this is only ok if we only calls getPass once when creating a singleton bean
        // if some clients call this method repeatedly we need to refactor it to somewhere else
        if (user != null && user.startsWith("encrypted:")) {
            try {
                user = Cryptor.decrypt(user.substring(10));
            } catch (Exception e) {
                logger.error("Failed to decrypt rabbitmq username from config file", e);
            }
        }
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @NotNull
    private String pass;

    public String getPass() {
        // this is only ok if we only calls getPass once when creating a singleton bean
        // if some clients call this method repeatedly we need to refactor it to somewhere else
        if (pass != null && pass.startsWith("encrypted:")) {
            try {
                pass = Cryptor.decrypt(pass.substring(10));
            } catch (Exception e) {
                logger.error("Failed to decrypt rabbitmq password from config file", e);
            }
        }
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    private long reconnectInterval;

    public long getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(long reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }
}
