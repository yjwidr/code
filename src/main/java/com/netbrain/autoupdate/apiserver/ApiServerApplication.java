package com.netbrain.autoupdate.apiserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableJpaAuditing
@ServletComponentScan(basePackages = {"com.netbrain.autoupdate.apiserver.filter"})
public class ApiServerApplication{
    private static Logger logger = LogManager.getLogger(ApiServerApplication.class.getName());

    public static void main(String[] args) {
        logger.debug("-------------------------debug-----------------------------");
        SpringApplication.run(ApiServerApplication.class, args);
    }
}
