package com.netbrain.autoupdate.apiagent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiAgentApplication{
    private static Logger logger = LogManager.getLogger(ApiAgentApplication.class.getName());

    public static void main(String[] args) {
        logger.debug("-------------------------debug-----------------------------");
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        SpringApplication.run(ApiAgentApplication.class, args);
    }
}
