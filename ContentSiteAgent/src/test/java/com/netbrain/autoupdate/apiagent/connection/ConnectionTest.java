package com.netbrain.autoupdate.apiagent.connection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.netbrain.autoupdate.apiagent.http.connection.Connection;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConnectionTest {
    @Autowired
    private Connection connection;
    @Test
    public void testConnect() throws Exception {
        connection.connectHttp("https://my.oschina.net/ososchina/blog/500925", null); 
    }
}
