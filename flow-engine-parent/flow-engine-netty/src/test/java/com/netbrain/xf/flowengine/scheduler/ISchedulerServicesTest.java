package com.netbrain.xf.flowengine.scheduler;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobDataMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.netbrain.xf.flowengine.scheduler.services.ISchedulerServices;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ISchedulerServicesTest {
    @Autowired
    private ISchedulerServices schedulerServices;

    @Test
    public void testGetjobDataMap() throws Exception {
        Map<String,JobDataMap> map = schedulerServices.getJobDataMap();
        Assert.assertNotNull(map);  
    }
    @Test
    public void testRunNow() throws Exception {
        String jobId="ad8f3440-b98f-082b-211c-e9e9af78a72e";
        Assert.assertEquals(schedulerServices.runNow(jobId),false);  
    }
    
    @Test
    public void testMakeScheduler() throws Exception {
        Map<String,JobDataMap> map =schedulerServices.getJobDataMap();
        Assert.assertNotNull(map);
        for(Entry<String, JobDataMap> entry:map.entrySet()) {
            JobDataMap jobDataMap=entry.getValue();
            schedulerServices.makeScheduler(jobDataMap);
        }
    }
}
