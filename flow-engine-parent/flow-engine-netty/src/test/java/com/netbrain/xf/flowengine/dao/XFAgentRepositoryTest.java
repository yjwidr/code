package com.netbrain.xf.flowengine.dao;

import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFDtg;
import com.netbrain.xf.xfcommon.XFCommon;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@RunWith(SpringRunner.class)
@SpringBootTest
public class XFAgentRepositoryTest {


    @Autowired
    private XFAgentRepository xfAgentRepository;

    @Before
    public void setUp() {
        xfAgentRepository.deleteAll();
    }

    @After
    public void after()
    {
        xfAgentRepository.deleteAll();
    }

    @Test
    public void test_Save() throws Exception {
        XFAgent xfagent1 = new XFAgent();
        String uuid1 = UUID.randomUUID().toString();
        xfagent1.setUniqIdForEachUpdate(uuid1);
        xfAgentRepository.save(xfagent1);
        Assert.assertEquals(1, xfAgentRepository.findAll().size());

        List<XFAgent> listAll = xfAgentRepository.findAll();
        XFAgent xfagent1_from_db = listAll.get(0);
        String uuid1_from_db = xfagent1_from_db.getUniqIdForEachUpdate();
        Assert.assertEquals(uuid1, uuid1_from_db );
    }

    @Test
    public void test_findAll() throws Exception {
        XFAgent xfagent1 = new XFAgent();
        String aUuid = UUID.randomUUID().toString();
        xfagent1.setUniqIdForEachUpdate(aUuid);
        xfAgentRepository.save(xfagent1);
        List<XFAgent> listAgent = this.xfAgentRepository.findAll();
        Assert.assertEquals(1, listAgent.size());

        XFAgent xfagnt1_found = listAgent.get(0);
        Assert.assertEquals(aUuid, xfagnt1_found.getUniqIdForEachUpdate());
    }
}
