package com.netbrain.xf.flowengine.queue;

public interface ITaskQueueManager {
    
    /**
     * An long running thread waiting for rpc requests and process them.
     */
    public void rpcHandleRequests();

    /**
     * Put a task into the TaskQueue
     * @param task, a pair of XFTask and AMQP.properties. When the AMQP.properties is null
     *              a new one is generated based on XFTask properties
     * @return
     */
    public boolean enqueue(TaskRequest task);

    public void addWorkerservers (String[] workerservernameArray);
}
