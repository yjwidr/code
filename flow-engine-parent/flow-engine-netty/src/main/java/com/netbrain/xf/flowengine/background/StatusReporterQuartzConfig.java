package com.netbrain.xf.flowengine.background;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.quartz.CronScheduleBuilder.cronSchedule;

@Configuration
public class StatusReporterQuartzConfig {
    @Value("${status.reporter.schedule}")
    private String reportSchedule;

    @Bean
    public JobDetail StatusReporterJobDetail() {
        return JobBuilder.newJob(StatusReporter.class).withIdentity("StatusReporterJob").storeDurably().build();
    }

    @Bean
    public Trigger StatusReporterJobTrigger() {
        return TriggerBuilder.newTrigger().forJob(StatusReporterJobDetail())
                .withIdentity("StatusReporterJobTrigger")
                .withSchedule(cronSchedule(reportSchedule).withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
