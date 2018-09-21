package com.netbrain.xf.flowengine.gateway;

import com.netbrain.xf.flowengine.taskcontroller.SubmitTaskResult;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.model.XFTask;
import com.rabbitmq.client.AMQP;
import org.junit.Assert;
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
public class AMQPTaskGatewayTest {
    @Autowired
    private AMQPTaskGateway amqpTaskGateway;

    private AMQP.BasicProperties prepMessageProperties() {
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

        AMQP.BasicProperties properties = new AMQP.BasicProperties("",
                "utf8", headers, 1, 170, "", "Nothing",
                "", "", new Date(), "", "guest", "XFClient", "cluster");
        return properties;
    }
    @Test
    public void testGenerateXFTask() throws Exception {
        String params = "{}";
        AMQP.BasicProperties properties = prepMessageProperties();

        XFTask submittedTask = amqpTaskGateway.generateXFTask(properties, params.getBytes());
        Assert.assertEquals("self_cb0e8c27-ccc0-4498-a955-b7fb19002f76", submittedTask.getId());
        Assert.assertEquals(3, submittedTask.getWorkerRestartTimes());
        Assert.assertEquals("d5260aaa-5aa4-3bb5-b50e-2fb3203735d4", submittedTask.getTenantId());
        Assert.assertEquals("fbcdb735-63b3-4736-b890-a53470f693b9", submittedTask.getDomainId());
        Assert.assertEquals("RMClientCallback_NB-DTP-284N_DefaultWebSite", submittedTask.getTaskCallbackQueue());
        Assert.assertEquals(10, submittedTask.getTaskPriority());
        Assert.assertFalse(submittedTask.isNeedBroadCallbackToAllApiServer());
    }

    @Test
    public void testGenerateXFTaskWithoutOptionalFields() throws Exception {
        // begin setup
        Map<String, Object> headers = new HashMap<>();

        headers.put("user_IPAddress", "::1");
        headers.put("user_name", "admin");
        headers.put("root_task_id", "root_cb0e8c27-ccc0-4498-a955-b7fb19002f76");

        //The following 3 might not be there, so this unit test need to be updated.
        headers.put("parent_task_id", "parent_cb0e8c27-ccc0-4498-a955-b7fb19002f76");
        headers.put("self_task_id", "self_cb0e8c27-ccc0-4498-a955-b7fb19002f76");
        headers.put("taskflow_id", "flow_cb0e8c27-ccc0-4498-a955-b7fb19002f76");

        headers.put("domainId", "fbcdb735-63b3-4736-b890-a53470f693b9");
        headers.put("tenantDbName", "T1");
        headers.put("domainDbName", "D1");
        headers.put("task_message_content_type", "task_message_content_type_task");
        headers.put("needBroadCallbackToAllApiServer", true);
        headers.put("task_priority", 10);
        headers.put("tenantId", "d5260aaa-5aa4-3bb5-b50e-2fb3203735d4");
        headers.put("task_job_id", "cb0e8c27-ccc0-4498-a955-b7fb19002f7");
        headers.put("task_type", "TestProxyServer");

        String params = "{}";
        AMQP.BasicProperties properties = new AMQP.BasicProperties("",
                "utf8", headers, 1, 170, "", "Nothing",
                "", "", new Date(), "", "guest", "XFClient", "cluster");
        TaskController mockedTaskController = mock(TaskController.class);
        amqpTaskGateway.setTaskController(mockedTaskController);
        // end of setup

        // test subject
        XFTask submittedTask = amqpTaskGateway.generateXFTask(properties, params.getBytes());

        Assert.assertEquals("self_cb0e8c27-ccc0-4498-a955-b7fb19002f76", submittedTask.getId());
        Assert.assertEquals(-2, submittedTask.getWorkerRestartTimes());
        Assert.assertTrue(submittedTask.isNeedBroadCallbackToAllApiServer());
        Assert.assertEquals("{}", submittedTask.getTaskParameters());
    }

    @Test
    public void testHandleNewTask() throws Exception {
        String params = "{}";
        AMQP.BasicProperties properties = prepMessageProperties();
        TaskController mockedTaskController = mock(TaskController.class);
        amqpTaskGateway.setTaskController(mockedTaskController);

        // test subject
        SubmitTaskResult submitResult = amqpTaskGateway.handleNewTask(properties, params.getBytes());
        verify(mockedTaskController).submitTask(any(XFTask.class), eq(properties));
    }

    @Test
    public void testHandleStopTask() throws Exception {
        // begin setup
        Map<String, Object> headers = new HashMap<>();

        int nTimeout = 1;
        String strReason = "Root Task b36cc1e4-04c8-4e40-8be6-71bc811d5d88 is ended by user via NetBrain Task Manager";
        headers.put("task_message_content_type", 5);
        headers.put("task_message_content_option", nTimeout);
        headers.put("ExecutionVehicle_cancel_task_reason", strReason);

        String params = "b36cc1e4-04c8-4e40-8be6-71bc811d5d88";
        AMQP.BasicProperties properties = new AMQP.BasicProperties("",
                "utf8", headers, 1, 170, "", "Nothing",
                "", "", new Date(), "", "guest", "XFClient", "cluster");
        TaskController mockedTaskController = mock(TaskController.class);
        amqpTaskGateway.setTaskController(mockedTaskController);
        // end of setup

        // test subject
        amqpTaskGateway.handleStopTaskflow(properties, params.getBytes());
        verify(mockedTaskController).stopTaskflowByJobIdOrTaskflowId(eq(params), eq(false), eq(nTimeout), eq(strReason) );
    }

}
