package com.netbrain.xf.flowengine;

import com.netbrain.xf.flowengine.metric.Metrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.netbrain.xf.flowengine.gateway.ITaskGateway;
import com.netbrain.xf.flowengine.utility.Cryptor;

/**
 * A spring boot application.
 */

// Disable auto configuration for mongodb properties.
// https://stackoverflow.com/questions/34414367/mongo-tries-to-connect-automatically-to-port-27017localhost
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class FlowEngineApplication implements CommandLineRunner {
    private static Logger logger = LogManager.getLogger(FlowEngineApplication.class.getSimpleName());
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Metrics metrics;

    @Override
    public void run(String... args) throws Exception {
        metrics.addStartCount(1);
        ITaskGateway taskGateway = applicationContext.getBean(ITaskGateway.class);
        taskGateway.initListener();
        taskGateway.handleRequests();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(FlowEngineApplication.class, args);
    }
}
