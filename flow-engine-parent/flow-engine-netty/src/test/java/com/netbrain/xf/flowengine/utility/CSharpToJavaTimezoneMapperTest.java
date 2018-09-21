package com.netbrain.xf.flowengine.utility;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZoneId;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CSharpToJavaTimezoneMapperTest {

    @Autowired
    CSharpToJavaTimezoneMapper timezoneMapper;

    @Test
    public void testGetZoneFromCSharpName() throws Exception {
        Assert.assertEquals(ZoneId.of("Etc/GMT+2"),
                timezoneMapper.getZoneFromCSharpName("UTC-02"));
    }

    @Test
    public void testGetZoneFromCSharpNameWithNull() throws Exception {
        Assert.assertEquals(ZoneId.systemDefault(),
                timezoneMapper.getZoneFromCSharpName(null));
    }

    @Test
    public void testGetZoneFromCSharpNameWithUnknownZone() throws Exception {
        Assert.assertEquals(ZoneId.of("America/New_York"),
                timezoneMapper.getZoneFromCSharpName("Microsoft Special time zone"));
    }
}
