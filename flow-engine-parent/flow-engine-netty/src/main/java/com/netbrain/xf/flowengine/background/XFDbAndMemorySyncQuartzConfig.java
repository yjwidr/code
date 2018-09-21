package com.netbrain.xf.flowengine.background;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XFDbAndMemorySyncQuartzConfig {
    @Value("${background.xfdbandmemorysync.syncinterval_in_seconds}")
    private int syncinterval_in_seconds;

    @Bean
    public JobDetail CreateXFDbAndMemorySyncQuartzJobDetail() {
        return JobBuilder.newJob(XFDbAndMemorySyncQuartzJob.class).withIdentity("XFDbAndMemorySyncQuartzJob").storeDurably().build();
    }

    @Bean
    public Trigger XFDbAndMemorySyncQuartzJobTrigger() {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(syncinterval_in_seconds)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(CreateXFDbAndMemorySyncQuartzJobDetail()).withIdentity("XFDbAndMemorySyncQuartzJobTrigger")
                .withSchedule(simpleScheduleBuilder).build();
    }
}
