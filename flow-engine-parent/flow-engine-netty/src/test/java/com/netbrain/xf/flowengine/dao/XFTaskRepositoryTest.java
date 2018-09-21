package com.netbrain.xf.flowengine.dao;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class XFTaskRepositoryTest {
    @Autowired
    private XFTaskRepository xfTaskRepository;

    @Autowired
    private XFTaskflowRepository xfTaskflowRepository;

    @Before
    public void setUp() {
        xfTaskRepository.deleteAll();
    }

    @After
    public void after(){
        xfTaskRepository.deleteAll();
    }

    @Test
    public void testSave() throws Exception {
        xfTaskRepository.save(new XFTask());
        Assert.assertEquals(1, xfTaskRepository.findAll().size());
    }

    @Test
    public void testfindRunningTasksForTaskflowId() throws Exception {

        String taskId = UUID.randomUUID().toString();
        String taskflowId = UUID.randomUUID().toString();

        XFTaskflow xfTaskFlow = new XFTaskflow();
        xfTaskFlow.setId(taskflowId);
        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId);
        xftask1.setXfTaskflow(xfTaskFlow);

        xftask1.setTaskStatus(XFCommon.TASKSTATUS_Started);

        xfTaskRepository.save(xftask1);
        this.xfTaskflowRepository.save(xfTaskFlow);
        //uuid1
        List<XFTask> foundXFTaskList =  xfTaskRepository.findRunningRootTasksForTaskflowId(taskflowId);
        Assert.assertEquals(1, foundXFTaskList.size());


        // search for a non-existent DtgId, result will be empty
        String uuid3 = UUID.randomUUID().toString();
        foundXFTaskList =  xfTaskRepository.findRunningRootTasksForTaskflowId(uuid3);
        Assert.assertEquals(0, foundXFTaskList.size());

        // set the task as completed
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_CompletedCrash);
        xfTaskRepository.save(xftask1);
        foundXFTaskList =  xfTaskRepository.findRunningRootTasksForTaskflowId(taskflowId);
        Assert.assertEquals(0, foundXFTaskList.size());
    }


    @Test
    public void testfindRunningTasksForDtgId() throws Exception {

        XFTaskflow xftaskflow1 = new XFTaskflow();
        String taskflowId = UUID.randomUUID().toString();
        xftaskflow1.setId(taskflowId);
        String taskId = UUID.randomUUID().toString();
        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId);

        List<String> assocDtgIds = new ArrayList<String>();

        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        assocDtgIds.add(uuid1);
        assocDtgIds.add(uuid2);
        xftask1.setAssociatedDtgIds(assocDtgIds);
        xftask1.setDtgId(uuid1);
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_Started);
        xftask1.setXfTaskflow(xftaskflow1);
        xfTaskRepository.save(xftask1);
        this.xfTaskflowRepository.save(xftaskflow1);

        //uuid1
        List<XFTask> foundXFTaskList =  xfTaskRepository.findRunningTasksForDtgId(uuid1);
        Assert.assertEquals(1, foundXFTaskList.size());

        //uuid2
        foundXFTaskList =  xfTaskRepository.findRunningTasksForDtgId(uuid2);
        Assert.assertEquals(1, foundXFTaskList.size());

        // search for a non-existent DtgId, result will be empty
        String uuid3 = UUID.randomUUID().toString();
        foundXFTaskList =  xfTaskRepository.findRunningTasksForDtgId(uuid3);
        Assert.assertEquals(0, foundXFTaskList.size());

        // set the task as completed
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_CompletedCrash);
        xfTaskRepository.save(xftask1);
        foundXFTaskList =  xfTaskRepository.findRunningTasksForDtgId(uuid1);
        Assert.assertEquals(0, foundXFTaskList.size());
    }


    @Test
    public void test_updateStatusToNonFinal() throws Exception {

        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(UUID.randomUUID().toString());
        String taskId = UUID.randomUUID().toString();
        XFTask xftask1 = new XFTask();

        xftask1.setId(taskId);
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_Canceled);
        xftask1.setXfTaskflow(xftaskflow1);
        this.xfTaskRepository.save(xftask1);
        this.xfTaskflowRepository.save(xftaskflow1);
        Optional<XFTask> xftask2 = xfTaskRepository.findById(taskId);
        Assert.assertEquals(XFCommon.TASKSTATUS_Canceled, xftask2.get().getTaskStatus());

        this.xfTaskRepository.updateStatus(xftask1, XFCommon.TASKSTATUS_Running, "", null);
        xftask2 = xfTaskRepository.findById(taskId);
        Assert.assertEquals(XFCommon.TASKSTATUS_Canceled, xftask2.get().getTaskStatus());


        String taskflowId1 = UUID.randomUUID().toString();
        xftaskflow1.setId(taskflowId1);
        xftaskflow1.setStatusPotentialDirty(false);
        xftask1.setXfTaskflow(xftaskflow1);

        xfTaskRepository.save(xftask1);
        xfTaskflowRepository.save(xftaskflow1);

        // update the status to a 'final' status, this will
        // set the dirty flag of taskflow and save it to DB
        xfTaskRepository.updateStatus(xftask1, XFCommon.TASKSTATUS_CompletedNormally, "", null);

        Optional<XFTaskflow> xftaskflow2 = xfTaskflowRepository.findById(xftask1.getTaskflowId());
        Assert.assertNotNull(xftaskflow2);
        Assert.assertEquals(true,xftaskflow2.get().getStatusPotentialDirty());
    }

    @Test
    public void test_updateStatusToFinal_noAssociatedTaskflow() throws Exception {
        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(UUID.randomUUID().toString());
        String taskId = UUID.randomUUID().toString();
        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId);
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_Started);
        this.xfTaskRepository.save(xftask1);
        Optional<XFTask> xftask2 = xfTaskRepository.findById(taskId);
        Assert.assertEquals(XFCommon.TASKSTATUS_Started, xftask2.get().getTaskStatus());

        boolean failedToUpdate = this.xfTaskRepository.updateStatus(xftask1, XFCommon.TASKSTATUS_CompletedNormally, "", null);
        Assert.assertEquals(false, failedToUpdate);
    }

    @Test
    public void test_updateTaskWhenXFAgentProcessCrashed() throws Exception {

        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(UUID.randomUUID().toString());
        String taskId = UUID.randomUUID().toString();
        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId);
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_Started);
        String serverName = "myServer";
        xftask1.setWorkerMachineName(serverName);
        int xfagentprocessid = 8888;
        xftask1.setXFAgentProcessId(xfagentprocessid);
        this.xfTaskRepository.save(xftask1);
        Optional<XFTask> xftask2 = xfTaskRepository.findById(taskId);

        Assert.assertEquals(XFCommon.TASKSTATUS_Started, xftask2.get().getTaskStatus());
        this.xfTaskRepository.updateTaskWhenXFAgentProcessCrashed(serverName, xfagentprocessid);

        xftask2 = xfTaskRepository.findById(taskId);
        Assert.assertEquals(XFCommon.TASKSTATUS_CompletedCrash, xftask2.get().getTaskStatus());

    }

    @Test
    public void test_uupdateTaskWhenXFAgentProcessNoUpdateTooLong() throws Exception {

        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(UUID.randomUUID().toString());
        String taskId = UUID.randomUUID().toString();
        XFTask xftask1 = new XFTask();
        xftask1.setId(taskId);
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_Started);
        String serverName = "myServer";
        xftask1.setWorkerMachineName(serverName);
        int xfagentprocessid = 8888;
        xftask1.setXFAgentProcessId(xfagentprocessid);
        this.xfTaskRepository.save(xftask1);
        Optional<XFTask> xftask2 = xfTaskRepository.findById(taskId);

        Assert.assertEquals(XFCommon.TASKSTATUS_Started, xftask2.get().getTaskStatus());
        this.xfTaskRepository.updateTaskWhenXFAgentProcessNoUpdateTooLong(serverName);

        xftask2 = xfTaskRepository.findById(taskId);
        Assert.assertEquals(XFCommon.TASKSTATUS_CompletedCrash, xftask2.get().getTaskStatus());

    }
}