package com.netbrain.xf.flowengine.background;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XFTaskMaintenanceQuartzConfig {
    @Value("${background.xftaskmaintenance.maintenanceinterval_in_seconds}")
    private int maintenanceinterval_in_seconds;

    @Bean
    public JobDetail CreateXFTaskMaintenanceQuartzJobDetail() {
        return JobBuilder.newJob(XFTaskMaintenanceQuartzJob.class).withIdentity("XFTaskMaintenanceQuartzJob").storeDurably().build();
    }

    @Bean
    public Trigger XFTaskMaintenanceQuartzJobTrigger() {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(maintenanceinterval_in_seconds)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(CreateXFTaskMaintenanceQuartzJobDetail()).withIdentity("XFTaskMaintenanceQuartzJobTrigger")
                .withSchedule(simpleScheduleBuilder).build();
    }
}
