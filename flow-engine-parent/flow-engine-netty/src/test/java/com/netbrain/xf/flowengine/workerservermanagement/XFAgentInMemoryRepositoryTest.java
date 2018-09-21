package com.netbrain.xf.flowengine.workerservermanagement;

import com.netbrain.xf.flowengine.dao.XFAgentRepository;
import com.netbrain.xf.model.XFAgent;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class XFAgentInMemoryRepositoryTest {

    @Autowired
    private XFAgentRepository xfAgentRepository;


    @Before
    public void setUp() {


    }

    @After
    public void after()
    {

    }

    @Test
    public void test_BacklistedXFagentWillNotBeSelected() throws Exception {

        Map<String,XFAgent> xfagentserverHashMap = new ConcurrentHashMap<String,XFAgent>();

        Map<String,XFAgentMetadata> xfagentserverMetadataHashMap = new ConcurrentHashMap<String,XFAgentMetadata>();

        String serverName = "serverName for testing " + UUID.randomUUID().toString();
        // create XFAgent, and put into server hash
        XFAgent agent = new XFAgent();
        agent.setServerName(serverName);
        xfagentserverHashMap.put(serverName, agent);

        XFAgentMetadata xfAgentMetadata = new XFAgentMetadata();
        xfAgentMetadata.setServerName(serverName);
        xfagentserverMetadataHashMap.put(serverName, xfAgentMetadata);


        List<XFAgent> listAgentsInMemory = new ArrayList<XFAgent>();
        for(Map.Entry<String, XFAgent> entry : xfagentserverHashMap.entrySet()) {
            XFAgent oneAgentInMemroy = entry.getValue();
            listAgentsInMemory.add(oneAgentInMemroy);
        }
        int origSize = listAgentsInMemory.size();

        // mark it as blacklisted
        xfAgentMetadata.setBlacklisted(true);
        xfAgentMetadata.setBlacklistedReasonCode(XFCommon.XFAgentBlacklistedReasonCode.REASON_DUE_TO_OVERLOAD);
        xfagentserverMetadataHashMap.put(serverName, xfAgentMetadata);

        listAgentsInMemory.removeIf((XFAgent agentInMem) ->{
            String xfagentName = agentInMem.getServerName();
            XFAgentMetadata metadata = xfagentserverMetadataHashMap.get(xfagentName);
            if (metadata.isBlacklisted() == true)
            {
                return true;
            }
            else
            {
                return false;
            }
        });

        int newSize = listAgentsInMemory.size();

        boolean bRemoved = (origSize - newSize) == 1;
        Assert.assertEquals(true, bRemoved);

    }

}



