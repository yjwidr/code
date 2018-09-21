package com.netbrain.autoupdate.apiagent.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import com.netbrain.autoupdate.apiagent.client.IEClient;
import com.netbrain.autoupdate.apiagent.constant.Constant;


@RunWith(SpringRunner.class)
@SpringBootTest
public class IEClientTest {
    @Autowired
    private IEClient ieClient;
    @Test
    public void testConnect() throws Exception {
//        ieClient.getCommand(Constant.ACD);
        ieClient.upload(Constant.ACP, getBytesByNio("test/package.zip"));
//        ieClient.upload("autoUpdate/contentSiteAgent/packages", CommonUtils.getBytes("conf/packagesZip.zip")) ;
//        List<ContentVersion> list = new ArrayList<>();
//        ContentVersion cve = new ContentVersion();
//        cve.setId("test1");
//        cve.setName("v7.1a init");
//        cve.setType((short)1);
//        list.add(cve);
//        ieClient.post("autoUpdate/contentSiteAgent/versions", JSON.toJSONString(list)) ;
//        String result=connection.get("autoUpdate/contentSiteAgent/command", null);
//        if(!StringUtils.isEmpty(result)) {
//            JSONObject json= JSON.parseObject(result);
//            String ResultCode= json.getJSONObject("operationResult").getString("ResultCode");
//        }
//        Map<String,Object> map = new HashMap<>();
//        map.put("errorType", 1);
//        map.put("errorSubType", 1);
//        map.put("errorMsg", "error");
//        map.put("errorDetails", "error details");
//        ieClient.post("autoUpdate/contentSiteAgent/error", JSON.toJSONString(map));
    }
    private byte[] getBytesByNio(String filename) throws IOException {
        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException(filename);
        }
        FileChannel channel = null;
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(f);
            channel = fs.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) channel.size());
            while ((channel.read(byteBuffer)) > 0) {
            }
            return byteBuffer.array();
        }finally {
            try {
                if(fs!=null) {
                    fs.close();
                }
                if(channel!=null ) {
                    channel.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}