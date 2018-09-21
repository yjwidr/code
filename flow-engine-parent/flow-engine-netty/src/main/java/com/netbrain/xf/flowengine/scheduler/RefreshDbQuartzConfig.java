package com.netbrain.xf.flowengine.scheduler;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefreshDbQuartzConfig {
    @Bean
    public JobDetail refreshDbJobDetail() {
        return JobBuilder.newJob(RefreshDbJob.class).withIdentity("refreshDbJob").storeDurably().build();
    }

    @Bean
    public Trigger refreshDbJobTrigger() {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(1)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(refreshDbJobDetail()).withIdentity("refreshDbTrigger")
                .withSchedule(simpleScheduleBuilder).build();
    }
}
