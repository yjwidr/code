package com.netbrain.xf.flowengine.dao;

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

import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class XFTaskflowRepositoryTest {
    @Autowired
    private XFTaskflowRepository xfTaskflowRepository;

    @Before
    public void setUp() {
        xfTaskflowRepository.deleteAll();
    }

    @After
    public void after(){
        xfTaskflowRepository.deleteAll();
    }

    @Test
    public void testExistsByJobIdAndStatusIn() throws Exception {
        String taskflowId = UUID.randomUUID().toString();

        XFTaskflow xfTaskFlow = new XFTaskflow();
        xfTaskFlow.setId(taskflowId);
        xfTaskFlow.setJobId("daily-benchmark");
        xfTaskflowRepository.save(xfTaskFlow);

        boolean foundWhenTaskSched = xfTaskflowRepository.existsByJobIdAndStatusIn("daily-benchmark", xfTaskFlow.getUnfinishedStates());
        Assert.assertTrue(foundWhenTaskSched);

        boolean foundForOtherJob = xfTaskflowRepository.existsByJobIdAndStatusIn("daily-discover", xfTaskFlow.getUnfinishedStates());
        Assert.assertFalse("Should not find a task when searching by other jobId", foundForOtherJob);

        xfTaskFlow.setStatus(XFTaskflow.STATUS_CompletedNormally);
        xfTaskflowRepository.save(xfTaskFlow);
        boolean foundWhenTaskCompletes = xfTaskflowRepository.existsByJobIdAndStatusIn("daily-benchmark", xfTaskFlow.getUnfinishedStates());
        Assert.assertFalse("Should not find an unfinished task when all tasks are done", foundWhenTaskCompletes);

    }
}
