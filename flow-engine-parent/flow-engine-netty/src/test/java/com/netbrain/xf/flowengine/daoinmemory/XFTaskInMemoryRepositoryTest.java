package com.netbrain.xf.flowengine.daoinmemory;

import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import com.netbrain.xf.xfcommon.XFCommon;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class XFTaskInMemoryRepositoryTest {
    @Autowired
    private XFTaskRepository xfTaskRepository;

    @Autowired
    private XFTaskflowRepository xfTaskflowRepository;

    @Autowired
    private XFTaskInMemoryRepository xfTaskInMemoryRepository;


    @Before
    public void setUp() {
        xfTaskInMemoryRepository.deleteAllFromMemoryAndDB();
        xfTaskRepository.deleteAll();
    }

    @After
    public void after()
    {
        xfTaskInMemoryRepository.deleteAllFromMemoryAndDB();
        xfTaskRepository.deleteAll();
    }

    @Test
    public void test_stringnull() throws Exception {

        String str1 = "hello";
        String str2 = null;

        boolean b1 = str1.equals(str2);
        //boolean b2 = str2.equals(str1);
        Assert.assertEquals(false, b1);
    }

    @Test
    public void test_findById() throws Exception {
        String taskId1 = UUID.randomUUID().toString();

        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId1);


        Optional<XFTask> xfTaskOpt = this.xfTaskInMemoryRepository.findById(taskId1);
        Assert.assertEquals(false, xfTaskOpt.isPresent());
        // Add xftask into InMemoryRepository
        this.xfTaskInMemoryRepository.upsertXFTask(taskId1, xftask1);
        xfTaskOpt = this.xfTaskInMemoryRepository.findById(taskId1);
        Assert.assertEquals(true, xfTaskOpt.isPresent());
        Assert.assertEquals(taskId1, xfTaskOpt.get().getId());
    }

    @Test
    public void testFindByTaskflow() throws Exception {
        String taskId1 = UUID.randomUUID().toString();

        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId1);
        XFTaskflow taskflow = new XFTaskflow();
        xftask1.setXfTaskflow(taskflow);

        List<XFTask> foundTasks = xfTaskInMemoryRepository.findByTaskflow(taskflow);
        Assert.assertEquals(0, foundTasks.size());
        // Add xftask into InMemoryRepository
        this.xfTaskInMemoryRepository.upsertXFTask(taskId1, xftask1);
        foundTasks = this.xfTaskInMemoryRepository.findByTaskflow(taskflow);
        Assert.assertEquals(1, foundTasks.size());
    }

    @Test
    public void test_syncFromMemoryToDB() throws Exception {
        String taskId1 = UUID.randomUUID().toString();
        String taskflowId1 = UUID.randomUUID().toString();

        XFTaskflow xfTaskFlow1 = new XFTaskflow();
        xfTaskFlow1.setId(taskflowId1);
        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId1);
        xftask1.setXfTaskflow(xfTaskFlow1);
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_Started);

        // Add xftask into InMemoryRepository
        this.xfTaskInMemoryRepository.upsertXFTask(taskId1, xftask1);


        // now verify that there is task in DB
        Optional<XFTask> xfTaskInDBOpt = this.xfTaskRepository.findById(taskId1);
        boolean bExistInDB = xfTaskInDBOpt.isPresent();
        Assert.assertTrue(bExistInDB);
        Assert.assertEquals(taskId1, xfTaskInDBOpt.get().getId());
    }

    @Test
    public void test_syncFromDBToMemory() throws Exception {
        String taskId1 = UUID.randomUUID().toString();
        String taskflowId1 = UUID.randomUUID().toString();

        XFTaskflow xfTaskFlow1 = new XFTaskflow();
        xfTaskFlow1.setId(taskflowId1);
        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId1);
        xftask1.setXfTaskflow(xfTaskFlow1);

        xftask1.setTaskStatus(XFCommon.TASKSTATUS_Started);

        xfTaskRepository.save(xftask1);
        this.xfTaskflowRepository.save(xfTaskFlow1);

        // first make sure there is no such task in InMemoryRepo
        XFTask xfTaskInMem = this.xfTaskInMemoryRepository.getXFTask(taskId1);
        Assert.assertNull(xfTaskInMem);

        this.xfTaskInMemoryRepository.syncFromDBToMemory(taskId1);

        // now verify that there is task in InMemoryRepo
        xfTaskInMem = this.xfTaskInMemoryRepository.getXFTask(taskId1);
        Assert.assertNotNull(xfTaskInMem);
        Assert.assertEquals(taskId1, xfTaskInMem.getId());
    }

    @Test
    public void test_deleteManyXFTaskForTaskflowIDs() throws Exception {

        this.xfTaskInMemoryRepository.deleteAllFromMemoryAndDB();
        String taskId1 = UUID.randomUUID().toString();
        String taskflowId1 = UUID.randomUUID().toString();

        XFTaskflow xfTaskFlow1 = new XFTaskflow();
        xfTaskFlow1.setId(taskflowId1);
        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId1);
        xftask1.setXfTaskflow(xfTaskFlow1);
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_Started);

        String taskId2 = UUID.randomUUID().toString();
        String taskflowId2 = UUID.randomUUID().toString();

        XFTaskflow xfTaskFlow2 = new XFTaskflow();
        xfTaskFlow2.setId(taskflowId2);
        XFTask xftask2 = new XFTask();
        xftask2.setId(taskId2);
        xftask2.setXfTaskflow(xfTaskFlow2);

        xftask2.setTaskStatus(XFCommon.TASKSTATUS_Canceled);

        this.xfTaskInMemoryRepository.upsertXFTask(xftask1.getId(), xftask1);
        this.xfTaskInMemoryRepository.upsertXFTask(xftask2.getId(), xftask2);
        Optional<XFTask> taskOpt = this.xfTaskInMemoryRepository.findById(xftask1.getId());
        Assert.assertTrue(taskOpt.isPresent());

        // Now delete it
        List<String > lstTaskflowIDs  = new ArrayList<String>();
        lstTaskflowIDs.add(taskflowId1);
        lstTaskflowIDs.add(taskflowId2);
        this.xfTaskInMemoryRepository.deleteManyXFTaskForTaskflowIDs(lstTaskflowIDs);

        taskOpt = this.xfTaskInMemoryRepository.findById(xftask1.getId(), false, false);
        Assert.assertFalse(taskOpt.isPresent());

        this.xfTaskInMemoryRepository.deleteAllFromMemoryAndDB();

    }

}
