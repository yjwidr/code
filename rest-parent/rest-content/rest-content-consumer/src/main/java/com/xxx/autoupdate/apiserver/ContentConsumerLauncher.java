package com.xxx.autoupdate.apiserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;

@SpringBootApplication
@EnableDubboConfiguration
public class ContentConsumerLauncher{
    public static void main(String[] args) {
        SpringApplication.run(ContentConsumerLauncher.class, args);
    }
}
