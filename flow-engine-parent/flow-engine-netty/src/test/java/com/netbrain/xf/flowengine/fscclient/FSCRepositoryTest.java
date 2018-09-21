package com.netbrain.xf.flowengine.fscclient;

import com.netbrain.ngsystem.model.FrontServerController;
import com.netbrain.xf.model.XFDtg;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FSCRepositoryTest {
    @Autowired
    private FSCRepository fscRepository;

    private FrontServerController prepFsc;

    @Before
    public void setUp() {
        prepFsc = new FrontServerController();
        prepFsc.setId("unique");
        //prepFsc.setIpOrHostname("10.0.0.1");
        fscRepository.addFSC(prepFsc);
    }

    @After
    public void tearDown() {
        fscRepository.removeFSC(prepFsc);
    }

    @Test
    public void testFindFSCByTenantIdForNonExistId() throws Exception {
        FrontServerController fsc = fscRepository.findFSCByTenantId("d5260aaa-5aa4-3bb5-b50e-2fb3203735d4");
        Assert.assertNull(fsc);
    }

    @Test
    public void testFindAllFSC() throws Exception {
        List<FrontServerController> fscs = fscRepository.findAll(100);
        Assert.assertTrue(!fscs.isEmpty());
    }
}
