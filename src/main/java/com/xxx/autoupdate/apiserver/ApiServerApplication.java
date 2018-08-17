package com.xxx.autoupdate.apiserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;

@SpringBootApplication
@EnableTransactionManagement
@EnableJpaAuditing
@ServletComponentScan(basePackages = {"com.xxx.autoupdate.apiserver.filter"})
@EnableDubboConfiguration
public class ApiServerApplication{
    private static Logger logger = LogManager.getLogger(ApiServerApplication.class.getName());

    public static void main(String[] args) {
        logger.debug("-------------------------debug-----------------------------");
        SpringApplication.run(ApiServerApplication.class, args);
    }
}
