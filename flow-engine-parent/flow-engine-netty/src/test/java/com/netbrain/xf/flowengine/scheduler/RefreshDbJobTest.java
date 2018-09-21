package com.netbrain.xf.flowengine.scheduler;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RefreshDbJobTest {
    @Autowired
    private Scheduler scheduler;
    @Resource(name="refreshDbJobTrigger")
    Trigger trigger;
    @Test
    public void testRefreshDbJob() throws Exception {
        Assert.assertEquals(scheduler.checkExists(trigger.getJobKey()),true);  
    }
}
