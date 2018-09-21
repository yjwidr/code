package com.netbrain.xf.flowengine.background;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DynamicConfigLoaderJobConfig {
    @Value("${background.config.loader.job.interval}")
    private int jobIntervalInSecond;

    @Bean
    public JobDetail DynamicConfigLoaderJobDetail() {
        return JobBuilder.newJob(DynamicConfigLoaderJob.class).withIdentity("DynamicConfigLoaderJob").storeDurably().build();
    }

    @Bean
    public Trigger  DynamicConfigLoaderJobTrigger() {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(jobIntervalInSecond)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(DynamicConfigLoaderJobDetail())
                .withIdentity("DynamicConfigLoaderJobTrigger")
                .withSchedule(simpleScheduleBuilder)
                .build();
    }
}
