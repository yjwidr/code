package com.netbrain.xf.flowengine.config;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {
        "com.netbrain.xf.flowengine.scheduler.services",
        "com.netbrain.xf.flowengine.fscclient"},
        mongoTemplateRef = "ngsystem")
public class NGSystemMongoTemplate {
    @Resource(name="ngsystemMongoProperties")
    private MongoProperties ngsystemMongoProperties;

    @Autowired
    NBMongoConfig nbMongoConfig;

    @Bean(name="ngsystem")
    public MongoTemplate ngsystemMongoTemplate() throws Exception {
        return new MongoTemplate(nbMongoConfig.buildMongoClient(), ngsystemMongoProperties.getDatabase());
    }
}