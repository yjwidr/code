package com.netbrain.autoupdate.apiserver.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import com.netbrain.autoupdate.apiserver.util.ContentVersionConvert;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UtilTest {

    
    @Test
    public void testSave() throws Exception {
        String a =ContentVersionConvert.contentVersionFromLongToStr(10200300000L);
        Assert.assertEquals("10.200.300000", a);
    }
}
