package com.netbrain.xf.flowengine.config;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RabbitmqConfigTest {

    @Autowired
    private RabbitmqConfig config;

    @Test
    public void testGetPass() throws Exception {
        RabbitmqConfig config = new RabbitmqConfig();
        config.setPass("encrypted:SwMgFTCStbV7W4dIRtw3MA==");
        Assert.assertEquals("mypassword", config.getPass());
    }
}
