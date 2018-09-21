package com.netbrain.xf.flowengine.background;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StaleDTGCheckerQuartzConfig {
    @Value("${background.staledtgchecker.job.interval}")
    private int jobIntervalInSecond;

    @Bean
    public JobDetail StaleDTGCheckerJobDetail() {
        return JobBuilder.newJob(StaleDTGChecker.class).withIdentity("StaleDTGCheckerJob").storeDurably().build();
    }

    @Bean
    public Trigger  StaleDTGCheckerJobTrigger() {
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(jobIntervalInSecond)
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(StaleDTGCheckerJobDetail())
                .withIdentity("StaleDTGCheckerJobTrigger")
                .withSchedule(simpleScheduleBuilder)
                .build();
    }
}
