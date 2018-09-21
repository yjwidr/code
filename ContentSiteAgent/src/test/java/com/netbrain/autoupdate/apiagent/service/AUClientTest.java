package com.netbrain.autoupdate.apiagent.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.netbrain.autoupdate.apiagent.client.AUClient;


@RunWith(SpringRunner.class)
@SpringBootTest
public class AUClientTest {
    @Autowired
    private AUClient auClient;
    @Test
    public void testDownload() throws Exception {
        byte[] bl=auClient.download("7.1a.zltest","/content/downloadMulti?sv=7.1a.zltest&cvr=99.20.0",null);
        int len = bl.length;
        System.out.println(len);
//        String result=httpHandler.get("/content/softewareVersionlist","netbrain");
//        Assert.assertEquals("success", null, null);
//        auClient.get("//content/list");
        
    }
}