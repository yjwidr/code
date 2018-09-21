package com.netbrain.xf.flowengine.config;

import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Since FlowEngine application needs to use two databases, NGSystem and its own flowengine,
 * we need to have separate MongoProperties for each.
 */
@Configuration
public class MultipleMongoProperties {
    @Bean
    @ConfigurationProperties(prefix = "mongodb.ngsystem")
    public MongoProperties ngsystemMongoProperties() {
        return new MongoProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "mongodb.flowengine")
    @Primary
    public MongoProperties flowengineMongoProperties() {
        return new MongoProperties();
    }

}
