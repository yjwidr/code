package com.netbrain.xf.flowengine.daoinmemory;

import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.UUID;
@RunWith(SpringRunner.class)
@SpringBootTest
public class XFTaskflowInMemoryRepositoryTest {

    @Autowired
    private XFTaskflowRepository xfTaskflowRepository;

    @Autowired
    private XFTaskflowInMemoryRepository xfTaskflowInMemoryRepository;

    @Before
    public void setUp() {
        xfTaskflowRepository.deleteAll();
        xfTaskflowInMemoryRepository.deleteAll();
    }

    @After
    public void after()
    {
        xfTaskflowRepository.deleteAll();
        xfTaskflowInMemoryRepository.deleteAll();
    }


    @Test
    public void test_findById() throws Exception {
        String taskflowId1 = UUID.randomUUID().toString();

        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(taskflowId1);

        Optional<XFTaskflow> xfTaskflowOpt = this.xfTaskflowInMemoryRepository.findById(taskflowId1, true, true);
        Assert.assertEquals(false, xfTaskflowOpt.isPresent());
        // Add xftask into InMemoryRepository
        this.xfTaskflowInMemoryRepository.upsertXFTaskflow(taskflowId1, xftaskflow1);
        xfTaskflowOpt = this.xfTaskflowInMemoryRepository.findById(taskflowId1, true, true);
        Assert.assertEquals(true, xfTaskflowOpt.isPresent());
        Assert.assertEquals(taskflowId1, xfTaskflowOpt.get().getId());
    }
}
