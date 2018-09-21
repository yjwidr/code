package com.netbrain.xf.flowengine.background;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XFAgentServerMonitorQuartzConfig {

    @Value("${background.xfagentservermonitor.monitoringinterval_in_seconds}")
    private int monitoringinterval_in_seconds;

    @Bean
    public JobDetail CreateXFAgentServerMonitorQuartzJobDetail() {
        return JobBuilder.newJob(XFAgentServerMonitorQuartzJob.class).withIdentity("XFAgentServerMonitorQuartzJob").storeDurably().build();
    }

    @Bean
    public Trigger XFAgentServerMonitorQuartzJobTrigger() {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(monitoringinterval_in_seconds)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(CreateXFAgentServerMonitorQuartzJobDetail()).withIdentity("XFAgentServerMonitorQuartzJobTrigger")
                .withSchedule(simpleScheduleBuilder).build();
    }
}
