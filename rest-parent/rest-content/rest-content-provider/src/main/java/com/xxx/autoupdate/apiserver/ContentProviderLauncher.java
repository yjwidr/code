package com.xxx.autoupdate.apiserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;

@SpringBootApplication
@EnableTransactionManagement
@EnableJpaAuditing
@EnableDubboConfiguration
public class ContentProviderLauncher{
    private static Logger logger = LogManager.getLogger(ContentProviderLauncher.class.getName());

    public static void main(String[] args) {
        logger.debug("-------------------------debug-----------------------------");
        SpringApplication.run(ContentProviderLauncher.class, args);
    }
}
