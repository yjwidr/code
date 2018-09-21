package com.netbrain.xf.flowengine.taskcontroller;

import com.netbrain.xf.flowengine.amqp.PublisherWithRetry;
import com.netbrain.xf.flowengine.dao.XFAgentRepository;
import com.netbrain.xf.flowengine.dao.XFDtgRepository;
import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskflowInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFAgentInMemoryRepository;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentMetadata;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFDtg;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import com.netbrain.xf.xfcommon.XFCommon;
import com.rabbitmq.client.AMQP;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TaskControllerTest {
    @Autowired
    private XFDtgRepository xfDtgRepository;

    @Autowired
    private XFTaskRepository taskRepository;

    @Autowired
    private XFTaskInMemoryRepository taskInMemoryRepository;

    @Autowired
    private XFTaskflowRepository xfTaskflowRepository;

    @Autowired
    private XFTaskflowInMemoryRepository taskflowInMemoryRepository;

    @Autowired
    private XFAgentRepository xfAgentRepository;

    @Autowired
    XFAgentInMemoryRepository xfAgentInMemoryRepository;

    @Autowired
    private TaskController taskController;

    @Autowired
    private PublisherWithRetry publisher;

    @Before
    public void setUp() {

    }

    @After
    public void after()
    {
    }

    @Test
    public void test_selectBestXFAgent_for_p1_task()
    {
        xfAgentRepository.deleteAll();

        long longmax = Long.MAX_VALUE;
        long fiveM = 5 *1024 * 1024;
        long oneGMem = 1 * 1024 * 1024 *1024;
        long oneGPlus5M = oneGMem + fiveM;
        long twoGMem = 2 * oneGMem;
        long threeGMem = 3 * oneGMem;
        // Case, there is no XFAGent record at all

        int pid1 = 1;
        int pid2 = 2;
        int pid3 = 3;

        XFAgent xfagent1 = new XFAgent();
        String aUuid1 = UUID.randomUUID().toString();
        xfagent1.setUniqIdForEachUpdate(aUuid1);
        long cpu1 = 10;

        xfagent1.setServerCpuPercent(cpu1);
        //https://stackoverflow.com/questions/6834037/initialize-a-long-in-java
        long p1Available1 = (long)10 * 1024 * 2014 * 1024;
        xfagent1.setP1AvailableByte(p1Available1);
        xfagent1.setP1IsOverloaded(false);
        xfagent1.setServerIsOverloaded(false);
        String strServerName1 = "Hello Server Name 1";
        xfagent1.setServerName(strServerName1);
        xfagent1.setXfAgentProcessId(pid1);
        xfAgentRepository.save(xfagent1);
        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(xfagent1);

        XFAgentMetadata newMeta = new XFAgentMetadata();
        newMeta.setServerName(strServerName1);
        //newMeta.setUniqIdForEachUpdate(matchedAgentInDB.getUniqIdForEachUpdate());
        Instant firstimeForThisUniqId = Instant.now();
        newMeta.setFirsttimeReceivedThisUniqId(firstimeForThisUniqId);
        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(newMeta);

        XFAgent xfagent2 = new XFAgent();
        String aUuid2 = UUID.randomUUID().toString();
        xfagent1.setUniqIdForEachUpdate(aUuid2);
        long cpu2 = 20;
        xfagent2.setServerCpuPercent(cpu2);
        long p1Available2 = (long)20 * 1024 * 2014 * 1024;
        xfagent2.setP1AvailableByte(p1Available2);
        xfagent2.setP1IsOverloaded(false);
        xfagent2.setServerIsOverloaded(false);
        String strServerName2 = "Hello Server Name 2";
        xfagent2.setServerName(strServerName2);
        xfagent2.setXfAgentProcessId(pid2);
        xfAgentRepository.save(xfagent2);
        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(xfagent2);

        newMeta.setServerName(strServerName2);
        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(newMeta);

        XFAgent xfagent3 = new XFAgent();
        String aUuid3 = UUID.randomUUID().toString();
        xfagent3.setUniqIdForEachUpdate(aUuid3);
        long cpu3 = 30;
        xfagent3.setServerCpuPercent(cpu3);

        long p1Available3 = (long)30 * 1024 * 2014 * 1024;
        xfagent3.setP1AvailableByte(p1Available3);
        xfagent3.setP1IsOverloaded(false);
        xfagent3.setServerIsOverloaded(false);
        String strServerName3 = "Hello Server Name 3";
        xfagent3.setServerName(strServerName3);
        xfagent3.setXfAgentProcessId(pid3);
        xfAgentRepository.save(xfagent3);
        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(xfagent3);

        newMeta.setServerName(strServerName3);
        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(newMeta);

        // case: p1 task should select the largest p1Availble xfagent
        XFTask taskToSend = new XFTask();
        taskToSend.setTaskPriority(XFCommon.TASK_PRIORITY_SUPER);
        XFCommon.XFAgentSelectionResult result = taskController.selectBestXFAgent(taskToSend);
        Assert.assertEquals(XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED, result.resultCode);
        Assert.assertEquals(String.format(XFCommon.XFAgent_task_queue_strformat, strServerName3), result.selectedQueueName);

        xfAgentRepository.deleteAll();
    }

    @Ignore
    @Test
    public void test_selectBestXFAgent()
    {
        xfAgentRepository.deleteAll();

        long fiveM = 5 *1024 * 1024;
        long oneGMem = 1 * 1024 * 1024 *1024;
        long oneGPlus5M = oneGMem + fiveM;
        long twoGMem = 2 * oneGMem;
        long threeGMem = 3 * oneGMem;
        // Case, there is no XFAGent record at all
        XFTask taskToSend = new XFTask();
        taskToSend.setTaskPriority(XFCommon.TASK_PRIORITY_SUPER);
        XFCommon.XFAgentSelectionResult result = taskController.selectBestXFAgent(taskToSend);
        Assert.assertEquals(XFCommon.XFAgentSelectionResultCode.RESULT_NO_XFAGENT, result.resultCode);

        XFAgent xfagent1 = new XFAgent();
        String aUuid1 = UUID.randomUUID().toString();
        xfagent1.setUniqIdForEachUpdate(aUuid1);
        long cpu1 = 80;
        xfagent1.setServerCpuPercent(cpu1);
        String strServerName1 = "Hello Server Name 1";
        xfagent1.setServerName(strServerName1);
        xfagent1.setServerIsOverloaded(false);
        xfagent1.setP1IsOverloaded(false);
        xfAgentRepository.save(xfagent1);
        List<XFAgent> listAgent = this.xfAgentRepository.findAll();
        Assert.assertEquals(1, listAgent.size());

        XFAgent xfagnt1_found = listAgent.get(0);
        Assert.assertEquals(aUuid1, xfagnt1_found.getUniqIdForEachUpdate());

        // Case: there is only 1 record
        result = taskController.selectBestXFAgent(taskToSend);
        Assert.assertEquals(XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED, result.resultCode);
        Assert.assertEquals(String.format(XFCommon.XFAgent_task_queue_strformat, strServerName1), result.selectedQueueName);

        // Case:there are 2 record
        XFAgent xfagent2 = new XFAgent();
        String aUuid2 = UUID.randomUUID().toString();
        xfagent2.setUniqIdForEachUpdate(aUuid2);
        String strServerName2 = "Hello Server Name 2";
        xfagent2.setServerName(strServerName2);
        xfagent2.setServerIsOverloaded(false);
        xfagent2.setP1IsOverloaded(false);
        long cpu2 = 60;
        xfagent2.setServerCpuPercent(cpu2);
        xfAgentRepository.save(xfagent2);
        listAgent = this.xfAgentRepository.findAll();
        Assert.assertEquals(2, listAgent.size());

        result = taskController.selectBestXFAgent(taskToSend);
        Assert.assertEquals(XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED, result.resultCode);
        Assert.assertEquals(String.format(XFCommon.XFAgent_task_queue_strformat, strServerName2), result.selectedQueueName);

        // Case:there are 3 records
        XFAgent xfagent3 = new XFAgent();
        String aUuid3 = UUID.randomUUID().toString();
        xfagent3.setUniqIdForEachUpdate(aUuid3);
        String strServerName3 = "Hello Server Name 3";
        xfagent3.setServerName(strServerName3);
        long cpu3 = 60;
        xfagent3.setServerCpuPercent(cpu3);
        xfagent3.setServerIsOverloaded(false);
        xfagent3.setP1IsOverloaded(false);
        xfAgentRepository.save(xfagent3);
        listAgent = this.xfAgentRepository.findAll();
        Assert.assertEquals(3, listAgent.size());

        cpu1 = 100;
        xfagent1.setServerCpuPercent(cpu1);
        xfagent1.setServerPhysicalAvailableMemoryInByte(threeGMem);
        cpu2 = 30;
        xfagent2.setServerCpuPercent(cpu2);
        xfagent2.setServerPhysicalAvailableMemoryInByte(twoGMem);
        cpu3 = 60;
        xfagent3.setServerCpuPercent(cpu3);
        xfagent3.setServerPhysicalAvailableMemoryInByte(oneGMem);
        xfAgentRepository.save(xfagent1);
        xfAgentRepository.save(xfagent2);
        xfAgentRepository.save(xfagent3);
        result = taskController.selectBestXFAgent(taskToSend);
        Assert.assertEquals(XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED, result.resultCode);
        Assert.assertEquals(String.format(XFCommon.XFAgent_task_queue_strformat, strServerName2), result.selectedQueueName);

        // Make cpu3 lowest, now we expect server3 to be selected.
        cpu3 = 10;
        xfagent3.setServerCpuPercent(cpu3);
        xfAgentRepository.save(xfagent3);
        result = taskController.selectBestXFAgent(taskToSend);
        Assert.assertEquals(XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED, result.resultCode);
        Assert.assertEquals(String.format(XFCommon.XFAgent_task_queue_strformat, strServerName3), result.selectedQueueName);


        // Now make their cpu all the same and compare the Available Physical memory
        cpu1 = 80;
        xfagent1.setServerCpuPercent(cpu1);
        xfagent1.setServerPhysicalAvailableMemoryInByte(threeGMem);
        cpu2 = 80;
        xfagent2.setServerCpuPercent(cpu2);
        xfagent2.setServerPhysicalAvailableMemoryInByte(twoGMem);
        cpu3 = 80;
        xfagent3.setServerCpuPercent(cpu3);
        xfagent3.setServerPhysicalAvailableMemoryInByte(oneGMem);
        xfAgentRepository.save(xfagent1);
        xfAgentRepository.save(xfagent2);
        xfAgentRepository.save(xfagent3);
        // when cpu are the same, the highest available physical memory will be selected, which is server 1
        result = taskController.selectBestXFAgent(taskToSend);
        Assert.assertEquals(XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED, result.resultCode);
        Assert.assertEquals(String.format(XFCommon.XFAgent_task_queue_strformat, strServerName1), result.selectedQueueName);

        // Case, cpu allowed error testing, expect the result still only depends on the memory
        int basecpu = 80;
        cpu1 = basecpu - XFCommon.AllowedDiffError_CPU;
        xfagent1.setServerCpuPercent(cpu1);
        xfagent1.setServerPhysicalAvailableMemoryInByte(threeGMem);
        cpu2 = basecpu - XFCommon.AllowedDiffError_CPU;;
        xfagent2.setServerCpuPercent(cpu2);
        xfagent2.setServerPhysicalAvailableMemoryInByte(twoGMem);
        cpu3 = basecpu;
        xfagent3.setServerCpuPercent(cpu3);
        xfagent3.setServerPhysicalAvailableMemoryInByte(oneGMem);
        xfAgentRepository.save(xfagent1);
        xfAgentRepository.save(xfagent2);
        xfAgentRepository.save(xfagent3);
        // when cpu are with the allowed error range, the highest available physical memory will be selected, which is server 1
        result = taskController.selectBestXFAgent(taskToSend);
        Assert.assertEquals(XFCommon.XFAgentSelectionResultCode.RESULT_SUCCEEDED, result.resultCode);
        Assert.assertEquals(String.format(XFCommon.XFAgent_task_queue_strformat, strServerName1), result.selectedQueueName);

        xfAgentRepository.deleteAll();
    }

    @Test
    public void testhasAnyRunningDtgsForDtgId() throws Exception {

        xfDtgRepository.deleteAll();
        String aDtgId = UUID.randomUUID().toString();
        XFDtg xfDtg1 = new XFDtg();
        xfDtg1.setId(aDtgId);

        xfDtgRepository.save(xfDtg1);

        // use reflection to call private method
        Class[] cArg = new Class[1];
        cArg[0] = String.class;
        Method method = taskController.getClass().getDeclaredMethod("hasAnyRunningDtgsForDtgId", cArg);
        method.setAccessible(true);
        boolean bHas =(boolean) method.invoke(taskController, aDtgId);

        //boolean bHas = taskController.hasAnyRunningDtgsForDtgId(aDtgId);
        Assert.assertEquals(true, bHas);

        xfDtgRepository.deleteAll();
    }

    @Test
    public void testhasAnyRunningTasksForDtgId() throws Exception {

        taskInMemoryRepository.deleteAllFromMemoryAndDB();
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

        this.taskInMemoryRepository.upsertXFTask(xftask1.getId(), xftask1);

        // use reflection to call private method
        Class[] cArg = new Class[1];
        cArg[0] = String.class;
        Method method = taskController.getClass().getDeclaredMethod("hasAnyRunningTasksForDtgId", cArg);
        method.setAccessible(true);

        // uuid1
        boolean bHas = (boolean) method.invoke(taskController, uuid1);
        Assert.assertEquals(true, bHas);

        //uuid2
        bHas = (boolean) method.invoke(taskController, uuid2);
        Assert.assertEquals(true, bHas);

        // search for a non-existent DtgId, result will be empty
        String uuid3 = UUID.randomUUID().toString();
        bHas = (boolean) method.invoke(taskController, uuid3);
        Assert.assertEquals(false, bHas);

        // set the task as completed
        xftask1.setTaskStatus(XFCommon.TASKSTATUS_CompletedCrash);
        this.taskInMemoryRepository.upsertXFTask(xftask1.getId(), xftask1);

        bHas = (boolean) method.invoke(taskController, uuid1);
        Assert.assertEquals(false, bHas);
        bHas = (boolean) method.invoke(taskController, uuid2);
        Assert.assertEquals(false, bHas);

        taskInMemoryRepository.deleteAllFromMemoryAndDB();
    }

    @Test
    public void test_getTaskFlowStatus() throws Exception {

        xfTaskflowRepository.deleteAll();
        String taskflowId = UUID.randomUUID().toString();
        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(taskflowId);

        // Create some XFTasks so that the flow status will be calculated as running
        XFTask xfTask1 = new XFTask();
        String xftaskId1 = UUID.randomUUID().toString();
        xfTask1.setId(xftaskId1);
        xfTask1.setXfTaskflow(xftaskflow1);
        taskInMemoryRepository.upsertXFTask(xftaskId1, xfTask1);

        Optional<XFTask> xftaskInDBOpt = this.taskRepository.findById(xftaskId1);
        Assert.assertTrue(xftaskInDBOpt.isPresent());

        taskflowInMemoryRepository.upsertXFTaskflow(taskflowId, xftaskflow1);
        int foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_Running, foundStatus);

        // Now set the xftask1 to be completed
        xfTask1.setTaskStatus(XFCommon.TASKSTATUS_CompletedNormally);
        this.taskInMemoryRepository.upsertXFTask(xftaskId1, xfTask1);
        if (xfTask1.getXfTaskflow().getStatusPotentialDirty())
        {
            this.taskflowInMemoryRepository.upsertXFTaskflow(xfTask1.getXfTaskflow().getId(), xfTask1.getXfTaskflow());
        }
        foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_CompletedNormally, foundStatus);
        // Now delete the xftask1, sleep the status will be

        xfTaskflowRepository.deleteAll();
    }

    @Test
    public void test_getTaskFlowStatus_withinShortInterval() throws Exception {

        xfTaskflowRepository.deleteAll();
        this.taskRepository.deleteAll();
        String taskflowId = UUID.randomUUID().toString();
        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(taskflowId);

        // Create some XFTasks so that the flow status will be calculated as running
        XFTask xfTask1 = new XFTask();
        String xftaskId1 = UUID.randomUUID().toString();
        xfTask1.setId(xftaskId1);
        xfTask1.setXfTaskflow(xftaskflow1);
        taskInMemoryRepository.upsertXFTask(xftaskId1, xfTask1);

        xfTaskflowRepository.save(xftaskflow1);
        int foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_Running, foundStatus);

        // Delete xfTask1
        taskInMemoryRepository.deleteXFTask(xftaskId1);
        this.taskRepository.deleteById(xftaskId1);
        Optional<XFTask> xftaskInDBOpt = this.taskRepository.findById(xftaskId1);
        Assert.assertTrue(xftaskInDBOpt.isPresent() == false);
        foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_Running, foundStatus);

        // sleep enough seconds, to force the status to be re calculated
        TimeUnit.SECONDS.sleep(TaskController.FLOWSTATUS_FORCE_REFRESH_TIME_INTERVAL +1);
        foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_CompletedNormally, foundStatus);

        xfTaskflowRepository.deleteAll();
        this.taskRepository.deleteAll();
    }

    @Test
    public void test_processTaskBeginMessage() throws Exception {

        xfTaskflowRepository.deleteAll();
        taskInMemoryRepository.deleteAllFromMemoryAndDB();
        String taskflowId = UUID.randomUUID().toString();
        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(taskflowId);

        // Create some XFTasks so that the flow status will be calculated as running
        XFTask xfTask1 = new XFTask();
        //xfTask1.setXfTaskflow(xftaskflow1);
        xfTask1.setTaskCallbackQueue("YNIU_RMClient_backkback");
        xfTask1.setTaskParameters("hello world parameter for begin testing");
        String xftaskId1 = taskflowId; //
        xfTask1.setId(xftaskId1);
        xfTask1.setXfTaskflow(xftaskflow1);
        xfTask1.setTaskStatus(XFCommon.TASKSTATUS_Running);
        //this.taskRepository.save(xfTask1);
        taskInMemoryRepository.upsertXFTask(xftaskId1, xfTask1);
        this.xfTaskflowRepository.save(xftaskflow1);
        int foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_Running, foundStatus);

        Map<String, Object> headers = new HashMap<>();
        headers.put(XFCommon.XFTaskflowId, taskflowId);
        AMQP.BasicProperties props = prepMessageProperties(headers);
        //String taskflowIdRcvd = amqpClient.extractStringHeader(headers, XFCommon.XFTaskflowId, "", true);
        //int taskCompleteStatus = amqpClient.extractGenericHeader(headers, XFCommon.complete_status, XFTask.TASKSTATUS_CompletedNormally, true);

        this.taskController.processTaskBeginMessage(xftaskId1, -1, null, props);

        XFTask xfTaskRet = this.taskInMemoryRepository.findById(xftaskId1).get();
        Assert.assertEquals(XFCommon.TASKSTATUS_Running, xfTaskRet.getTaskStatus());

        xfTaskflowRepository.deleteAll();
        taskInMemoryRepository.deleteAllFromMemoryAndDB();
    }


    @Test
    public void test_processTaskEndMessage() throws Exception {

        xfTaskflowRepository.deleteAll();
        taskRepository.deleteAll();
        String taskflowId = UUID.randomUUID().toString();
        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(taskflowId);

        // Create some XFTasks so that the flow status will be calculated as running
        XFTask xfTask1 = new XFTask();
        xfTask1.setTaskCallbackQueue("YNIU_RMClient_backkback");
        xfTask1.setTaskParameters("hello world parameter");
        String xftaskId1 =taskflowId; // has to do this
        xfTask1.setId(xftaskId1);
        xfTask1.setXfTaskflow(xftaskflow1);
        taskInMemoryRepository.upsertXFTask(xftaskId1, xfTask1);
        this.taskflowInMemoryRepository.upsertXFTaskflow(xftaskflow1.getId(), xftaskflow1);
        int foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_Running, foundStatus);

        Map<String, Object> headers = new HashMap<>();
        headers.put(XFCommon.XFTaskflowId, taskflowId);
        headers.put(XFCommon.complete_status, (int)XFCommon.TASKSTATUS_CompletedNormally);

        Map<String, Object> amqpHeaders = prepHeaders(headers);
        AMQP.BasicProperties mockedProps = mock(AMQP.BasicProperties.class);
        when(mockedProps.getHeaders()).thenReturn(amqpHeaders);

        PublisherWithRetry mockedPublisher = mock(PublisherWithRetry.class);
        Mockito.doNothing().when(mockedPublisher).publishWithRetry(any(), any(), eq(mockedProps), any());
        taskController.setPublisher(mockedPublisher);

        // XFAgent should update XFTask status before publishing task end message, simulate this action here
        xfTask1.setTaskStatus(XFCommon.TASKSTATUS_CompletedNormally);
        taskInMemoryRepository.upsertXFTask(xftaskId1, xfTask1);

        this.taskController.processTaskEndMessage(xftaskId1, XFCommon.TASKSTATUS_CompletedNormally);

        XFTask xfTaskRet = this.taskInMemoryRepository.findById(xftaskId1).get();
        String taskflowIdRet = xfTaskRet.getTaskflowId();

        TimeUnit.SECONDS.sleep(TaskController.FLOWSTATUS_FORCE_REFRESH_TIME_INTERVAL +1);
        int flowStatus = this.taskController.CalculateTasklowStatus(taskflowIdRet);
        XFTaskflow xfTaskflowRet = this.taskflowInMemoryRepository.findById(taskflowIdRet,true, true).get();
        int flowStatusNew = xfTaskflowRet.getStatus();
        Assert.assertEquals(true, xfTaskflowRet.getStatusIsFinal());

        Assert.assertEquals(XFTaskflow.STATUS_CompletedNormally, mockedProps.getHeaders().get("task_complete_status"));
        xfTaskflowRepository.deleteAll();
        taskInMemoryRepository.deleteAllFromMemoryAndDB();
    }

    @Test
    public void test_hasAnyRunningRootTasksForTaskflowId() throws Exception
    {
        xfTaskflowRepository.deleteAll();
        taskRepository.deleteAll();
        taskInMemoryRepository.deleteAllFromMemoryAndDB();
        taskflowInMemoryRepository.deleteAll();
        String taskflowId = UUID.randomUUID().toString();
        XFTaskflow xftaskflow1 = new XFTaskflow();
        xftaskflow1.setId(taskflowId);

        // Create some XFTasks so that the flow status will be calculated as running
        XFTask  rootxfTask1 = new XFTask();
        rootxfTask1.setTaskCallbackQueue("YNIU_RMClient_backkback");
        rootxfTask1.setTaskParameters("hello world parameter");
        String xftaskId1 =taskflowId; // has to do this
        rootxfTask1.setId(xftaskId1);
        rootxfTask1.setRootTaskId(xftaskId1);
        rootxfTask1.setTaskStatus(XFCommon.TASKSTATUS_Running);
        rootxfTask1.setXfTaskflow(xftaskflow1);
        taskInMemoryRepository.upsertXFTask(xftaskId1, rootxfTask1);
        this.taskflowInMemoryRepository.upsertXFTaskflow(xftaskflow1.getId(), xftaskflow1);

        // Child Task 1
        XFTask xfTask_child1 = new XFTask();
        String child1_taskid = UUID.randomUUID().toString();
        xfTask_child1.setId(child1_taskid);
        xfTask_child1.setParentTaskId(xftaskId1);
        taskInMemoryRepository.upsertXFTask(xfTask_child1.getId(), xfTask_child1);

        xftaskflow1.setStatusPotentialDirty(true);
        this.taskflowInMemoryRepository.upsertXFTaskflow(xftaskflow1.getId(), xftaskflow1);
        int foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_Running, foundStatus);

        // update child task status, this should not affect the calculation of the taskflow status, which is only related with root tasks
        xfTask_child1.setTaskStatus(XFCommon.TASKSTATUS_CompletedNormally);
        taskInMemoryRepository.upsertXFTask(xfTask_child1.getId(), xfTask_child1);
        xftaskflow1.setStatusPotentialDirty(true);
        this.taskflowInMemoryRepository.upsertXFTaskflow(xftaskflow1.getId(), xftaskflow1);
        foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_Running, foundStatus);


        // now if we update the root task 1 to a final status, the taskflow status will also be final
        rootxfTask1.setTaskStatus(XFCommon.TASKSTATUS_CompletedNormally);
        taskInMemoryRepository.upsertXFTask(rootxfTask1.getId(), rootxfTask1);
        xftaskflow1.setStatusPotentialDirty(true);
        this.taskflowInMemoryRepository.upsertXFTaskflow(xftaskflow1.getId(), xftaskflow1);
        foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_CompletedNormally, foundStatus);

        // Now, generate root task 2, mark it as running, the taskflow status should be running
        XFTask rootxfTask2 = new XFTask();
        String roottaskid2 = UUID.randomUUID().toString();
        rootxfTask2.setId(roottaskid2);
        rootxfTask2.setRootTaskId(roottaskid2);
        rootxfTask2.setTaskStatus(XFCommon.TASKSTATUS_Running);
        rootxfTask2.setXfTaskflow(xftaskflow1);
        taskInMemoryRepository.upsertXFTask(rootxfTask2.getId(), rootxfTask2);

        // reset the taskflow status to some initial value
        xftaskflow1.setStatus(XFCommon.TASKFLOWSTATUS_Unknown);
        xftaskflow1.setStatusPotentialDirty(true);
        this.taskflowInMemoryRepository.upsertXFTaskflow(xftaskflow1.getId(), xftaskflow1);
        foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_Running, foundStatus);

        // now if we update the root task 2 to a final status, the taskflow status will also be final
        rootxfTask2.setTaskStatus(XFCommon.TASKSTATUS_Canceled);
        taskInMemoryRepository.upsertXFTask(rootxfTask2.getId(), rootxfTask2);
        xftaskflow1.setStatusPotentialDirty(true);
        this.taskflowInMemoryRepository.upsertXFTaskflow(xftaskflow1.getId(), xftaskflow1);
        foundStatus = taskController.CalculateTasklowStatus(taskflowId);
        Assert.assertEquals(XFTaskflow.STATUS_CompletedNormally, foundStatus);

        xfTaskflowRepository.deleteAll();
        taskInMemoryRepository.deleteAllFromMemoryAndDB();
        taskflowInMemoryRepository.deleteAll();
    }

    private Map<String, Object> prepHeaders(Map<String, Object> given_headers) {
        Map<String, Object> headers = new HashMap<>();

        headers.put("user_IPAddress", "::1");
        headers.put("user_name", "admin");
        headers.put("WorkerRestartTimes", 3);
        headers.put("root_task_id", "cb0e8c27-ccc0-4498-a955-b7fb19002f76");

        //The following 3 might not be there, so this unit test need to be updated.
        headers.put("parent_task_id", "parent_cb0e8c27-ccc0-4498-a955-b7fb19002f76");
        headers.put("self_task_id", "self_cb0e8c27-ccc0-4498-a955-b7fb19002f76");
        headers.put("taskflow_id", "flow_cb0e8c27-ccc0-4498-a955-b7fb19002f76");

        headers.put("shortDescription", "");
        headers.put("domainId", "fbcdb735-63b3-4736-b890-a53470f693b9");
        headers.put("tenantDbName", "T1");
        headers.put("domainDbName", "D1");
        headers.put("task_message_content_type", "task_message_content_type_task");
        headers.put("jobRunCategory", "RunAsOnDemandJob");
        headers.put("needBroadCallbackToAppApiServer", false);
        headers.put("task_priority", 10);
        headers.put("tenantId", "d5260aaa-5aa4-3bb5-b50e-2fb3203735d4");
        headers.put("task_callback_queue", "RMClientCallback_NB-DTP-284N_DefaultWebSite");
        headers.put("task_job_id", "cb0e8c27-ccc0-4498-a955-b7fb19002f7");
        headers.put("task_type", "TestProxyServer");

        for (Map.Entry<String,Object> pair : given_headers.entrySet()){
            //iterate over the pairs
            headers.put(pair.getKey(), pair.getValue());
        }

        return headers;
    }
    private AMQP.BasicProperties prepMessageProperties(Map<String, Object> given_headers) {
        AMQP.BasicProperties properties = new AMQP.BasicProperties("",
                "utf8", prepHeaders(given_headers), 1, 170, "", "Nothing",
                "", "", new Date(), "", "guest", "XFClient", "cluster");
        return properties;
    }

}