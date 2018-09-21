package com.netbrain.xf.flowengine.gateway;

import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.xfcommon.XFCommon;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TaskStatusMessageHandlerTest {
    @Autowired
    TaskStatusMessageHandler taskStatusMessageHandler;

    private AMQP.BasicProperties prepMessageProperties(String taskStatusType) {
        Map<String, Object> headers = new HashMap<>();

        headers.put("user_IPAddress", "::1");
        headers.put("user_name", "admin");
        headers.put("WorkerRestartTimes", 3);
        headers.put("root_task_id", "cb0e8c27-ccc0-4498-a955-b7fb19002f76");

        headers.put("self_task_id", "self_cb0e8c27-ccc0-4498-a955-b7fb19002f76");
        headers.put("root_task_id", "root_cb0e8c27-ccc0-4498-a955-b7fb19002f76");
        headers.put("taskflow_id", "flow_cb0e8c27-ccc0-4498-a955-b7fb19002f76");

        headers.put("domainId", "fbcdb735-63b3-4736-b890-a53470f693b9");
        headers.put("tenantDbName", "T1");
        headers.put("domainDbName", "D1");
        headers.put("task_message_content_type", taskStatusType);
        headers.put("jobRunCategory", "RunAsOnDemandJob");
        headers.put("tenantId", "d5260aaa-5aa4-3bb5-b50e-2fb3203735d4");
        headers.put("task_type", "TestProxyServer");
        headers.put("task_complete_status", XFCommon.TASKSTATUS_CompletedNormally);
        AMQP.BasicProperties properties = new AMQP.BasicProperties("",
                "utf8", headers, 1, 170, "", "Nothing",
                "", "", new Date(), "", "guest", "XFClient", "cluster");
        return properties;
    }

    @Test
    public void testHandleBeginMessage() throws Exception {
        String params = "{}";
        Channel mockedChannel = mock(Channel.class);
        TaskController mockedTaskController = mock(TaskController.class);
        Envelope mockedEnvelope = new Envelope(101, false, "test-exchange", "test-routing-key");
        AMQP.BasicProperties properties = prepMessageProperties("task_message_content_type_execution_begin");
        taskStatusMessageHandler.setTaskController(mockedTaskController);

        taskStatusMessageHandler.handleMessage("test-queue", mockedChannel, "test-gateway", mockedEnvelope, properties, "".getBytes());
        verify(mockedChannel).basicAck(101, false);
        verify(mockedTaskController).processTaskBeginMessage(eq("self_cb0e8c27-ccc0-4498-a955-b7fb19002f76"), eq(-1), eq(null), any());
    }

    @Test
    public void testHandleEndMessage() throws Exception {
        String params = "{}";
        Channel mockedChannel = mock(Channel.class);
        TaskController mockedTaskController = mock(TaskController.class);
        Envelope mockedEnvelope = new Envelope(101, false, "test-exchange", "test-routing-key");
        AMQP.BasicProperties properties = prepMessageProperties("task_message_content_type_execution_end");
        taskStatusMessageHandler.setTaskController(mockedTaskController);

        taskStatusMessageHandler.handleMessage("test-queue", mockedChannel, "test-gateway", mockedEnvelope, properties, "".getBytes());
        verify(mockedChannel).basicAck(101, false);
        verify(mockedTaskController).processTaskEndMessage(eq("self_cb0e8c27-ccc0-4498-a955-b7fb19002f76"), eq(XFCommon.TASKSTATUS_CompletedNormally));
    }
}
