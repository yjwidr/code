package com.netbrain.xf.flowengine.fscclient;

import com.netbrain.xf.flowengine.dao.XFDtgRepository;
import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskflowInMemoryRepository;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.model.XFDtg;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import com.rabbitmq.client.AMQP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AMQPTriggerReceiverTest {

    @Autowired
    AMQPTriggerReceiver amqpTriggerReceiver;

    XFTaskflowInMemoryRepository mockedTFRepo;
    XFTaskInMemoryRepository mockedTaskRepo;
    XFDtgRepository mockedDtgRepo;

    private AMQP.BasicProperties prepMessageProperties() {
        Map<String, Object> headers = new HashMap<>();

        headers.put(AMQPTriggerReceiver.TRIGGER_MSG_ID, "99cdb735-63b3-4736-b890-a53470f693b9");
        headers.put(AMQPTriggerReceiver.TRIGGER_MSG_DTGID, "88cdb735-63b3-4736-b890-a53470f693b9");
        headers.put(AMQPTriggerReceiver.TRIGGER_MSG_FINAL, true);

        AMQP.BasicProperties properties = new AMQP.BasicProperties("",
                "utf8", headers, 1, 170, "", "Nothing",
                "", "", new Date(), "", "guest", "XFClient", "cluster");
        return properties;
    }

    @Before
    public void setUp() {
        mockedTFRepo = mock(XFTaskflowInMemoryRepository.class);
        mockedTaskRepo = mock(XFTaskInMemoryRepository.class);
        mockedDtgRepo = mock(XFDtgRepository.class);
        amqpTriggerReceiver.setDtgRepository(mockedDtgRepo);
        amqpTriggerReceiver.setTaskflowRepository(mockedTFRepo);
        amqpTriggerReceiver.setTaskInMemoryRepository(mockedTaskRepo);
    }

    @Test
    public void testGenerateXFTaskForInvalidDTG() throws Exception {
        when(mockedDtgRepo.findById("88cdb735-63b3-4736-b890-a53470f693b9")).thenReturn(Optional.empty());

        XFTask submittedTask = amqpTriggerReceiver.generateXFTask("trigger-id", "88cdb735-63b3-4736-b890-a53470f693b9", false);
        Assert.assertNull("Task should not be generated", submittedTask);
    }

    @Test
    public void testGenerateXFTaskForInvalidFlow() throws Exception {
        XFDtg dtg = new XFDtg();
        dtg.setTaskflowId("no-what-you-are-expecting");
        when(mockedDtgRepo.findById("88cdb735-63b3-4736-b890-a53470f693b9")).thenReturn(Optional.of(dtg));

        XFTask submittedTask = amqpTriggerReceiver.generateXFTask("trigger-id", "88cdb735-63b3-4736-b890-a53470f693b9", false);
        Assert.assertNull("Task should not be generated", submittedTask);
    }

    @Test
    public void testGenerateXFTask() throws Exception {
        String flowId = "77cdb735-63b3-4736-b890-a53470f693b9";

        XFDtg dtg = new XFDtg();
        dtg.setTriggeredTaskParameters("{\"ping\":\"10.10.1.1\"}");
        dtg.setTriggeredTaskType("run-ping");
        dtg.setTaskflowId(flowId);
        XFTaskflow mockedTaskFlow = new XFTaskflow();
        mockedTaskFlow.setId(flowId);
        XFTask initTask = new XFTask();
        initTask.setId(flowId);
        initTask.setXfTaskflow(mockedTaskFlow);

        when(mockedDtgRepo.findById("88cdb735-63b3-4736-b890-a53470f693b9")).thenReturn(Optional.of(dtg));
        when(mockedTFRepo.findById(flowId, true, true)).thenReturn(Optional.of(mockedTaskFlow));
        when(mockedTaskRepo.findById(flowId)).thenReturn(Optional.of(initTask));

        XFTask submittedTask = amqpTriggerReceiver.generateXFTask("trigger-id", "88cdb735-63b3-4736-b890-a53470f693b9", false);
        Assert.assertNotNull("Task should be generated", submittedTask);
        Assert.assertEquals("run-ping", submittedTask.getTaskType());
        Assert.assertEquals("{\"ping\":\"10.10.1.1\"}", submittedTask.getTaskParameters());
    }
}
