package com.netbrain.xf.flowengine.config;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.netbrain.xf.flowengine.dao",mongoTemplateRef = "flowEngine")
public class FlowEngineMongoTemplate {
    @Resource(name="flowengineMongoProperties")
    private MongoProperties flowengineMongoProperties;
    
    @Autowired
    NBMongoConfig nbMongoConfig;

    @Bean(name="flowEngine")
    @Primary
    public MongoTemplate flowEngineMongoTemplate() throws Exception {
        return new MongoTemplate(nbMongoConfig.buildMongoClient(), flowengineMongoProperties.getDatabase());
    }
}