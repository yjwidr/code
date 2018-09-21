package com.netbrain.xf.flowengine.queue;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.netbrain.xf.model.XFTask;

import java.util.concurrent.PriorityBlockingQueue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ITaskQueueManagerTest {
    @Autowired
    private TaskQueueManagerImpl taskQueueManager;

    @Test
    public void testPriorityTasks() throws Exception {
        for(int i = 0; i < 5; i++) {
            XFTask task = new XFTask();
            task.setTaskPriority(i);
            taskQueueManager.enqueue(new TaskRequest(task, null));
        }

        TaskRequest taskRequest = taskQueueManager.getTaskQueue().take();
        Assert.assertEquals(4, taskRequest.getXfTask().getTaskPriority());

        taskRequest = taskQueueManager.getTaskQueue().take();
        Assert.assertEquals(3, taskRequest.getXfTask().getTaskPriority());

        taskRequest = taskQueueManager.getTaskQueue().take();
        Assert.assertEquals(2, taskRequest.getXfTask().getTaskPriority());

        taskRequest = taskQueueManager.getTaskQueue().take();
        Assert.assertEquals(1, taskRequest.getXfTask().getTaskPriority());

        taskRequest = taskQueueManager.getTaskQueue().take();
        Assert.assertEquals(0, taskRequest.getXfTask().getTaskPriority());
    }

    @Test
    public void testPriorityTasksWithDifferentLevel() throws Exception {

        XFTask task1 = new XFTask();
        task1.setId("1");
        task1.setTaskPriority(1);
        task1.setTaskLevelFromRoot(99);
        taskQueueManager.enqueue(new TaskRequest(task1, null));

        XFTask task2 = new XFTask();
        task2.setId("2");
        task2.setTaskPriority(2);
        task2.setTaskLevelFromRoot(88);
        taskQueueManager.enqueue(new TaskRequest(task2, null));

        // same taskpriority but different level with task2
        XFTask task3 = new XFTask();
        task3.setId("3");
        task3.setTaskPriority(2);
        task3.setTaskLevelFromRoot(77);
        taskQueueManager.enqueue(new TaskRequest(task3, null));

        XFTask task4= new XFTask();
        task4.setId("4");
        task4.setTaskPriority(4);
        task4.setTaskLevelFromRoot(0);
        taskQueueManager.enqueue(new TaskRequest(task4, null));

        TaskRequest taskRequest = taskQueueManager.getTaskQueue().take();
        Assert.assertEquals("4", taskRequest.getXfTask().getId());

        taskRequest = taskQueueManager.getTaskQueue().take();
        Assert.assertEquals("2", taskRequest.getXfTask().getId());

        taskRequest = taskQueueManager.getTaskQueue().take();
        Assert.assertEquals("3", taskRequest.getXfTask().getId());

        taskRequest = taskQueueManager.getTaskQueue().take();
        Assert.assertEquals("1", taskRequest.getXfTask().getId());

    }

    @Test
    public void test_PriorityQueueBounded() throws Exception {

        int limit = 5;
        PriorityBlockingQueue<String> taskQueue = new PriorityBlockingQueue<String>(5);
        boolean bAdded = false;
        for (int i =0; i < 5; i++)
        {
            bAdded = taskQueue.add(Integer.toString(5 - i));
            Assert.assertTrue(bAdded);
        }

        bAdded = true;
        if (taskQueue.size() >= limit)
        {
            bAdded = false;
        }
        else
        {
            bAdded = taskQueue.add("one more");
        }

        Assert.assertFalse(bAdded);

    }
}
