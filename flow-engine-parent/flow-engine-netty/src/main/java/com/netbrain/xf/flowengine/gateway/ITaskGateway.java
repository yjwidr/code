package com.netbrain.xf.flowengine.gateway;

/**
 * Task Gateway interface. Task Gateway is used to serve requests from XFClient (previously called RMClient)
 */
public interface ITaskGateway {

//    public static final String TASK_TYPE_HEADER_KEY = "task_message_content_type";
//    public static final String TASK_TYPE_NEW_TASK = "task_message_content_type_task";
//    public static final String TASK_TYPE_NEW_SUB_TASK = "task_message_content_type_sub_task";

    /**
     * Initialize a listener.
     * For AMQP protocol it creates a connection and channel, and hooks up a consumer
     * For TCP, it creates a server socket and wait for connections from a XFClient.
     * @return 0 as success
     */
    public int initListener();

    /**
     * An long running thread waiting for requests and process them.
     */
    public void handleRequests();
}
