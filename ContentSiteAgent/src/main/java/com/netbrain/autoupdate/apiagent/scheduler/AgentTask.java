package com.netbrain.autoupdate.apiagent.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.netbrain.autoupdate.apiagent.service.AgentService;

@Component
public class AgentTask {

	@Autowired
	private AgentService agentService;
	
    @Value("${agent.scheduler.time}")
    private int schedulerTime;
	
	
	@PostConstruct
	public void init() {
		ScheduledExecutorService scheduExec = Executors.newScheduledThreadPool(1);
		scheduExec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    agentService.proxy();
                } catch (Exception e) {
                	agentService.uploadError(e);
                }
            }
        }, 0,schedulerTime*1000, TimeUnit.MILLISECONDS);
	}	
}
