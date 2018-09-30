package com.xxx.autoupdate.apiserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;

@SpringBootApplication
@EnableDubboConfiguration
public class WhiteListConsumerLauncher{
    public static void main(String[] args) {
        SpringApplication.run(WhiteListConsumerLauncher.class, args);
    }
}
