package com.netbrain.xf.flowengine.config;

import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.validation.constraints.NotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.netbrain.xf.flowengine.utility.CommonUtil;
import com.netbrain.xf.flowengine.utility.Cryptor;

import io.netty.handler.ssl.JdkSslClientContext;

@Component
@ConfigurationProperties(prefix="netbrain.mongodb")
@Validated
public class NBMongoConfig {
    private static Logger logger = LogManager.getLogger(AMQPClient.class.getName());
    private boolean ssl;
    private String cert_verification;
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
    private String servers;

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String replicaSet;

    public String getReplicaSet() {
        return replicaSet;
    }

    public void setReplicaSet(String replicaSet) {
        this.replicaSet = replicaSet;
    }

    public MongoClient buildMongoClient() throws Exception {
        List<MongoCredential> credentialsList = null;
        MongoClient mongoClient = null;

        String[] servers = getServers().split(",");
        List<ServerAddress> serverAddresses = new ArrayList<>(servers.length);
        for (String server: servers) {
            String[] entry = server.split(":");
            if (entry.length != 2) {
                throw new IllegalArgumentException("Invalid mongodb server address format: " + server + ". Should be hostname:port");
            }
            serverAddresses.add(new ServerAddress(entry[0], Integer.parseInt(entry[1])));
        }

        MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
        if (isSsl()) {
            if(getCert_verification().equalsIgnoreCase("verify_ca_no_limit")) {
                File certFile = CommonUtil.getCertFile();
                if(!ObjectUtils.isEmpty(certFile)) {
                    JdkSslClientContext jdkSslClientContext= new JdkSslClientContext(certFile);
                    optionsBuilder.sslEnabled(true).sslInvalidHostNameAllowed(true).sslContext(jdkSslClientContext.context());
                }else {
                    JdkSslClientContext jdkSslClientContext = new JdkSslClientContext(CommonUtil.getTrustManager());
                    optionsBuilder.sslEnabled(true).sslInvalidHostNameAllowed(true).sslContext(jdkSslClientContext.context());
                }
            }else if(getCert_verification().equalsIgnoreCase("none")) {
                optionsBuilder.sslEnabled(true).sslInvalidHostNameAllowed(true).socketFactory(getSslSocketFactory());
            }
        }
        if (!StringUtils.isEmpty(getReplicaSet())) {
            optionsBuilder.requiredReplicaSetName(this.getReplicaSet());
        }

        if(!StringUtils.isEmpty(this.getUsername())) {
            String username = getUsername();
            if (username != null && username.startsWith("encrypted:")) {
                username = Cryptor.decrypt(username.substring(10));
            }

            String password = this.getPassword();
            if (password != null && password.startsWith("encrypted:")) {
                password = Cryptor.decrypt(password.substring(10));
            }

            credentialsList = new ArrayList<>();
            credentialsList.add(MongoCredential.createCredential(username,
                    "admin",
                    password.toCharArray()));

            if (serverAddresses.size() > 1) {
                // replica set
                mongoClient = new MongoClient(serverAddresses, credentialsList, optionsBuilder.build());
            } else {
                mongoClient = new MongoClient(serverAddresses.get(0), credentialsList,optionsBuilder.build());
            }
        }else{
            // no credentail
            if (serverAddresses.size() > 1) {
                mongoClient = new MongoClient(serverAddresses, optionsBuilder.build());
            } else {
                // single mongo
                mongoClient = new MongoClient(serverAddresses.get(0));
            }
        }
        return mongoClient;
    }
    private static SSLSocketFactory getSslSocketFactory() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Couldn't create tls Context for MongoDB connection", e);
            throw new RuntimeException(e);
        }
        return sslContext.getSocketFactory();
    }
}
