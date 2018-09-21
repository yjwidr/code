package com.netbrain.xf.flowengine.background;

import com.netbrain.xf.flowengine.amqp.AMQPClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DynamicConfigLoaderJobTest {
    @Autowired
    private DynamicConfigLoaderJob dynamicConfigLoaderJob;

    @Test
    public void testLoadWorkerserver() throws Exception {
        List<String> workerServers = dynamicConfigLoaderJob.loadWorkerservers();
        Assert.assertNotNull(workerServers);
    }
}