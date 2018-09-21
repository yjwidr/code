package com.netbrain.xf.flowengine.metric;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefreshMetric {
    @Value("${taskengine.metric.interval}")
    private int interval;
    
    @Bean
    public JobDetail refreshMetricJobDetail() {
        return JobBuilder.newJob(RefreshMetricJob.class).withIdentity("refreshMetricJob").storeDurably().build();
    }

    @Bean
    public Trigger refreshMetricJobTrigger() {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(refreshMetricJobDetail()).withIdentity("refreshMetricTrigger")
                .withSchedule(simpleScheduleBuilder).build();
    }
}
