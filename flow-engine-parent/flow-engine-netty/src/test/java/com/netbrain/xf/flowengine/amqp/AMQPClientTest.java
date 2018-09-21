package com.netbrain.xf.flowengine.amqp;

import com.rabbitmq.client.Address;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AMQPClientTest {
    @Autowired
    private AMQPClient amqpClientBean;

    @Test
    public void testInitBeanWithBadHostname() throws Exception {
        // Assert.assertEquals("no", amqpClientBean.isRabbitCaught());
    }
}
