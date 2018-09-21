package com.netbrain.xf.flowengine.scheduler;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DelayedTaskScanJobConfig {
    @Bean
    public JobDetail delayedTaskScanJobDetail() {
        return JobBuilder.newJob(DelayedTaskScanJob.class).withIdentity("delayedTaskScanJob").storeDurably().build();
    }

    @Bean
    public Trigger delayedTaskScanJobTrigger() {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(1)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(delayedTaskScanJobDetail()).withIdentity("delayedTaskScanJobTrigger")
                .withSchedule(simpleScheduleBuilder).build();
    }
}