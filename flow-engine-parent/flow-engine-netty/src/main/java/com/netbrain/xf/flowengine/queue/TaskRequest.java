package com.netbrain.xf.flowengine.queue;

import com.netbrain.xf.model.XFTask;
import com.rabbitmq.client.AMQP;

import java.time.Instant;

/**
 * A POJO class holding together an XFTask and AMQP properties sent by XFClient.
 */
public class TaskRequest implements Comparable<TaskRequest> {

    private XFTask xfTask;

    private AMQP.BasicProperties amqpMsgProperties;

    public XFTask getXfTask() {
        return xfTask;
    }

    public void setXfTask(XFTask xfTask) {
        this.xfTask = xfTask;
    }

    public AMQP.BasicProperties getAmqpMsgProperties() {
        return amqpMsgProperties;
    }

    public void setAmqpMsgProperties(AMQP.BasicProperties amqpMsgProperties) {
        this.amqpMsgProperties = amqpMsgProperties;
    }

    public TaskRequest(XFTask task, AMQP.BasicProperties properties) {
        this.xfTask = task;
        this.amqpMsgProperties = properties;
    }

    /**
     * The reverse of natural order of priority.
     * i.e. When comparing priority 10 with priority 1, this comparator returns -1
     * so that priority 10 is de-queued before priority 1.
     * @param other
     * @return
     */
    @Override
    public int compareTo(TaskRequest other) {
        int result = 0;
        if (this.xfTask != null && other.xfTask != null) {

            if (this.xfTask.getTaskPriority() > other.xfTask.getTaskPriority()) {
                result = -1;
            } else if (this.xfTask.getTaskPriority() < other.xfTask.getTaskPriority()) {
                result = 1;
            }
            else // if they are equal
            {
                int thisLevel = this.xfTask.getTaskLevelFromRoot();
                int otherLevel = other.xfTask.getTaskLevelFromRoot();
                if (thisLevel > otherLevel)
                {
                    result = -1;
                }
                else if (thisLevel < otherLevel)
                {
                    result = 1;
                }
                else // if still the same we compare the task's submittime
                {
                    Instant submitTimeThis = this.xfTask.getSubmitTime();
                    Instant submitTimeOther = other.xfTask.getSubmitTime();
                    if (submitTimeThis != null && submitTimeOther != null)
                    {
                        if (submitTimeThis.isBefore(submitTimeOther))
                        {
                            result = -1;
                        }
                        else if (submitTimeThis.isAfter(submitTimeOther))
                        {
                            result = 1;
                        }
                    }
                }

            }
        }
        return result;
    }
}
