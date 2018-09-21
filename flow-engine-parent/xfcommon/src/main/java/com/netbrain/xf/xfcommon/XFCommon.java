package com.netbrain.xf.xfcommon;

public class XFCommon {

    public static String NBMSGVERSION = "NB_MSG_VERSION";
    public static String NBMSGVERSION_NB_IE_7_DOT_1 = "NB_IE7.1";
    public static final String NBMSG_MINOR_VERSION_KEY = "NB_MSG_MinorVersion";
    public static final int NBMSG_MINOR_VERSION_VALUE = 1;

    public static int CANCELTASKFLOW_TIMEOUT_OVERHEAD_INSECONDS = 2;

    public static String XFAgent_queuename_suffix = "_xfagent";

    public static String XFAgent_task_queue_strformat = "nb_xfagent_task_%s_queue"; // %s need to be replaced by worker server name

    public static String str_task_reply_to = "task_reply_to";
    public static String complete_status = "task_complete_status";

    public static final String FLOWENGINE_EXCHANGE = "nb_flowengine_exchange";

    public static final String TRIGGER_EXCHANGE_NAME = "fsc_trigger_exchange";
    public static final String TRIGGER_QUEUE_NAME_V1 = "fsc_trigger_queue";

    public static String XFTaskExchange = "XFTaskExchange";
    public static String XFTaskQueue = "XFTaskQueue";
    public static String XFTaskDataTaskGroup = "XFTaskDataTaskGroup";
    public static String XFTaskRefid = "task_ref_id";
    public static String XFTaskflowId = "taskflow_id";
    public static String XFTaskResult = "XFTaskResult";
    public static String XFTaskReplyQueueNameV1 = "xfagent_replyto";
    public static String XFTaskReplyQueueName = "nb_flowengine_xfagentreply_queue";

    public static final String root_task_id = "root_task_id";
    public static final String parent_task_id = "parent_task_id";
    public static final String self_task_id = "self_task_id";

    public static String client_request_exchange = "client_request_exchange";
    public static String callback_queue = "task_callback_queue";

    public static String RMCanceledTaskDBName = "NGSystem";
    public static String RMCanceledTaskCollectionName = "RMCanceledTask";
    public static String cancel_task_timeout = "ExecutionVehicle_cancel_task_timeout";
    public static String cancel_task_reason = "ExecutionVehicle_cancel_task_reason";
    public static String command_cancel_task = "ExecutionVehicle_cancel_task";
    public static String command_RMAgent_please_suicide_now = "command_RMAgent_please_suicide_now";
    public static String RMAgent_suicide_reason = "RMAgent_suicide_reason";
    public static String RMAgent_exchange = "RMAgent_exchange";
    public static String RMAgent_command = "RMAgent_command";

    public static String IDToCancel = "IDToCancel";
    public static String IDToCancelType = "IDToCancelType";
    public static String IDType_JobId = "IDType_JobId";
    public static String IDType_TaskflowId = "IDType_TaskflowId";
    public static String IDType_TaskId = "IDType_TaskId";


    public static String DBSTR_XFAgentProcessId = "xfAgentProcessId";
    public static String DBSTR_XFTASK_CANCELEDREASON = "canceledReason";
    public static String DBSTR_XFTASK_TASKSTATUSFINALREASON = "taskStatusFinalReason";

    public static String DBSTR_XFTASK_TASKSTATUS = "taskStatus";
    public static String DBSTR_XFTASK_TASKTYPE = "taskType";
    public static String DBSTR_XFTASK_PARENTTASKID = "parentTaskId";
    public static String DBSTR_XFTASK_TASKFLOWID = "taskflowId";
    public static String DBSTR_XFTASK_ROOTTASKID = "rootTaskId";
    public static String DBSTR_XFTASK_ASSOCIATEDDTGIDS = "associatedDtgIds";
    public static String DBSTR_XFTASK_WORKERPROCESSID = "workerProcessId";
    public static String DBSTR_XFTASK_WORKERMACHINENAME = "workerMachineName";
    public static String DBSTR_XFTASK_WORKERRESTARTTIMES = "workerRestartTimes";
    public static String DBSTR_XFTASK_WORKERRESTARTTIMEUSED = "workerRestartTimesUsed";
    public static String DBSTR_XFTASK_WORKERISRESTART = "workerIsRestart";

    public static String DBSTR_XFTASK_SHORTDESCRIPTION = "shortDescription";
    public static String DBSTR_XFTASK_WAITEDBYPARENT = "waitedByParent";
    public static String DBSTR_XFTASK_SUBMITTIME = "submitTime";
    public static String DBSTR_XFTASK_STARTTIME = "startTime";
    public static String DBSTR_XFTASK_ENDTIME = "endTime";
    public static String DBSTR_XFTASK_EXPIRETIME = "expireTime";
    public static String DBTR_XFTASK_USERNAME = "userName";
    public static String STR_xfAgentRunningInstanceId = "xfAgentRunningInstanceId";

    public static final int TASK_PRIORITY_SUPER = 10;
    public static final int TASK_PRIORITY_HIGH = 2;
    public static final int TASK_PRIORITY_LOW = 1;
    public static final int TASK_RABBITMQ_PRIORITY_SUPER = 170;
    public static final int TASK_RABBITMQ_PRIORITY_HIGH = 85;
    public static final int TASK_RABBITMQ_PRIORITY_LOW = 0;

    public static final int TASKSTATUS_Unknown = 0;
    public static final int TASKSTATUS_Scheduled = 1;
    public static final int TASKSTATUS_Started = 2;
    public static final int TASKSTATUS_Running = 3;
    public static final int TASKSTATUS_CompletedNormally = 4;
    public static final int TASKSTATUS_CompletedWithException = 5;
    public static final int TASKSTATUS_CompletedCrash = 6;
    public static final int TASKSTATUS_Canceled = 7;
    public static final int TASKSTATUS_MergeAndSkip = 8;

    public static final int DTGSTATUS_Unknown = 0;
    public static final int DTGSTATUS_Started = 2;
    public static final int DTGSTATUS_Running = 3;
    public static final int DTGSTATUS_CompletedNormally = 4; // last trigger has been received
    public static final int DTGSTATUS_Canceled = 7;
    public static final int DTGSTATUS_NonExist = 9; // FSC cannot find this DTG any more, probably caused by FSC change

    public static final int TASKFLOWSTATUS_Unknown = 0;
    public static final int TASKFLOWSTATUS_Started = 2;
    public static final int TASKFLOWSTATUS_Running = 3;
    public static final int TASKFLOWSTATUS_CompletedNormally = 4;
    public static final int TASKFLOWSTATUS_Canceled = 7;

    public static String XFAGENT_PROCESS_ID = "xfagentProcessId";
    public static String XFAGENT_SERVER_NAME = "xfagentServerName";

    public static String MSG_KEY_ROOT_TASK_ID = "root_task_id";
    public static String MSG_KEY_PARENT_TASK_ID = "parent_task_id";
    public static String MSG_KEY_SELF_TASK_ID = "self_task_id";
    public static String MSG_KEY_TASKFLOW_ID = "taskflow_id";
    public static String MSG_KEY_ASSOC_DTG_IDS = "associatedDtgIds";
    public static String MSG_KEY_ANCESTOR_TASK_IDS = "ancestor_task_ids";

    public static class XFAgentBlacklistedReasonCode
    {
        public static final int REASON_NOT_BLACKLISTED = 0;
        public static final int REASON_DUE_TO_OVERLOAD = 10;

        public static final int REASON_DUE_TO_NO_UPDATE_TOO_LONG = 20;
        public static final int REASON_DUE_TO_XFAGENT_RESTARTED = 30;
    }

    // xftothink, rename this to XFResultCode
    public static class XFAgentSelectionResultCode
    {
        public static final int RESULT_SUCCEEDED = 0;
        public static final int RESULT_NO_XFAGENT = 1;
        public static final int RESULT_NO_AVAILABLE_XFAGENT = 3;

        public static final int SENDRESULT_XFAGENT_TASK_EXPIRED = 100;

    }

    public static class XFAgentSelectionAlgorithmType
    {
        public static final int ByLowerCPUAndHigherAvailablePhysicalMemoryInByte = 1;
        public static final int ByLowerCPUAndLowerAvailablePhysicalMemoryPercentage = 2;
    }

    public static final int DefaultAndGuessedXFAgentHeartBeatIntervalInSeconds = 20; // better be the same value in XFAgent side
    public static final int AllowedDiffError_VirtualMemory = 10 * 1024 * 1024; // 10M
    public static final int AllowedDiffError_CPU = 5;
    public static final long UNKNOWN_CPU = 100 + AllowedDiffError_CPU + 1; // if it is unknown, we should NOT choose it
    public static final long UNKNOWN_PhysicalTotalMemoryInByte = 0;
    public static final long UNKNOWN_PhysicalAvailableMemoryInByte = 0;
    public static final int AllowedDiffError_AvailablePhysicalMemoryInByte = 10 *1024 *1024; // 10M

    public static final long EstimatedXFTaskVirtualMemory = (long)250 * 1024 *1024; // when a task is selected for a best XFAgent, need to subtract this much virtual memory

    public static class XFAgentSelectionResult
    {
        public int resultCode = XFAgentSelectionResultCode.RESULT_NO_XFAGENT;
        public String selectedWorkerServerName = "";
        public String selectedQueueName = "";
    }

    public static class RabbitMqString
    {
        public static String queue_max_length = "x-max-length";
        public static String queue_max_length_bytes = "x-max-length-bytes";
        public static String x_dead_letter_routing_key = "x-dead-letter-routing-key";
        public static String x_dead_letter_exchange = "x-dead-letter-exchange";
        public static String EXCHANGE_TYPE_DIRECT = "direct";
        public static String EXCHANGE_TYPE_FANOUT = "fanout";
        public static String EXCHANGE_TYPE_TOPIC = "topic";
        public static String EXCHANGE_TYPE_HEADERS = "headers";
        public static String MSG_TYPE_JSON = "application/json";
    }

    public static class XFMessageTypes
    {
        public static String MSGTYPE_XFAgent_HeartBeat = "RMAgent_heart_beat";
        public static String MSGTYPE_XFAgent_Crashed = "RMAgent_crash_detected";
        public static String MSGTYPE_XFAgent_ShuttingDown = "RMAgent_is_shuttingdown";
    }

    public static class XFFieldTypes
    {
        public static String FieldTYPE_XFAgentProcessId = "xfAgentProcessId";
        public static String FieldTYPE_WorkerServerHostName = "workerServerHostName";
    }

    public static class XFAgentInformationFrom
    {
        public static int FROM_DB_UPDATE = 0;
        public static int FROM_RABBITMQ_HB_MESSAGE = 1;
        public static int FROM_RABBITMQ_OTHER_MESSAGE = 2;
    }
}
