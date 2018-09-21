package com.netbrain.xf.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.netbrain.xf.xfcommon.XFCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Document(collection = "XFTask")
@CompoundIndex(def = "{'workerMachineName':1, 'xfAgentProcessId':1, 'taskStatus' :1}", name = "workerMachineName_xfAgentProcessId_taskStatus_compound_index")
public class XFTask {
    private static Logger logger = LogManager.getLogger(XFTask.class.getSimpleName());

    public static final int WorkerRestartTimesInfinite = -1; // Timeout.Infinite is also -1
    public static final int WorkerRestartTimes_Use_WorkerTypeConfig = -2;

    public static final String CATEGORY_OnDemand = "RunAsOnDemandJob";
    public static final String CATEGORY_Scheduled = "RunAsScheduledJob";

    public boolean getStatusIsFinal()
    {
        return (this.taskStatus > XFCommon.TASKSTATUS_Running);
    }

    private String id;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public XFTask() {
    }

    public String ToLogString()
    {
        StringBuilder sb = new StringBuilder(300);
        sb.append("jobId=" + this.jobId + ";");
        sb.append("taskflowId=" + this.taskflowId + ";");
        sb.append("rootTaskId=" + this.rootTaskId + ";");
        sb.append("parentTaskId=" + this.parentTaskId + ";");
        sb.append("selfTaskId=" + this.id + ";");
        sb.append("taskType=" + this.taskType + ";");

        return sb.toString();
    }

    public String toSummaryString()
    {
        StringBuilder sb = new StringBuilder(300);
        sb.append("jobId=" + this.jobId + ";");
        sb.append("taskflowId=" + this.taskflowId + ";");
        sb.append("rootTaskId=" + this.rootTaskId + ";");
        sb.append("parentTaskId=" + this.parentTaskId + ";");
        sb.append("selfTaskId=" + this.id + ";");
        sb.append("taskType=" + this.taskType + ";");
        sb.append("submitTime=" + this.getSubmitTime() + ";");
        sb.append("dispatchTime=" + this.getDispatchTime() + ";");
        sb.append("startTime=" + this.getStartTime() + ";");
        sb.append("endTime=" + this.getEndTime() + ";");

        return sb.toString();
    }

    public boolean isRootTask()
    {
        boolean bIsRoot = (this.parentTaskId == null || this.parentTaskId.isEmpty());
        return bIsRoot;
    }
    public String getSelfTaskId() {
        return id;
    }

    public String getRootTaskId() {
        return rootTaskId;
    }

    public void setRootTaskId(String rootTaskId) {
        this.rootTaskId = rootTaskId;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getTaskflowId() {
        if (this.xfTaskflow == null)
        {
            return this.taskflowId;
        }
        return this.xfTaskflow.getId();
    }

    public void setTaskflowId(String taskflowId) {
        if (this.xfTaskflow == null)
        {
            this.taskflowId = taskflowId;
        }
        else
        {
            String strErr = "setTaskFlowId should not be called after taskflow is already associated with this xftask id = " + this.id + " taskflowId = " + taskflowId;
            logger.error(strErr);
            throw new IllegalStateException(strErr);
        }
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public int getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(int taskPriority) {
        this.taskPriority = taskPriority;
    }

    public int getTaskRabbitmqPriority() {
        return taskRabbitmqPriority;
    }

    public void setTaskRabbitmqPriority(int taskRabbitmqPriority) {
        this.taskRabbitmqPriority = taskRabbitmqPriority;
    }

    public int getTaskLevelFromRoot() {
        return taskLevelFromRoot;
    }

    public void setTaskLevelFromRoot(int taskLevelFromRoot) {
        this.taskLevelFromRoot = taskLevelFromRoot;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserIP() {
        return userIP;
    }

    public void setUserIP(String userIP) {
        this.userIP = userIP;
    }

    public int getWorkerRestartTimes() {
        return workerRestartTimes;
    }

    public void setWorkerRestartTimes(int workerRestartTimes) {
        this.workerRestartTimes = workerRestartTimes;
    }

    public int getWorkerRestartTimesUsed() {
        return workerRestartTimesUsed;
    }

    public void setWorkerRestartTimesUsed(int workerRestartTimesUsed) {
        this.workerRestartTimesUsed = workerRestartTimesUsed;
    }

    public boolean isWorkerIsRestart() {
        return workerIsRestart;
    }

    public void setWorkerIsRestart(boolean workerIsRestart) {
        this.workerIsRestart = workerIsRestart;
    }

    public int getWorkerProcessId() {
        return workerProcessId;
    }

    public void setWorkerProcessId(int workerProcessId) {
        this.workerProcessId = workerProcessId;
    }

    public String getWorkerMachineName() {
        return workerMachineName;
    }

    public void setWorkerMachineName(String workerMachineName) {
        this.workerMachineName = workerMachineName;
    }

    public String getJobRunCategory() {
        return jobRunCategory;
    }

    public void setJobRunCategory(String jobRunCategory) {
        this.jobRunCategory = jobRunCategory;
    }

    public String getTaskParameters() {
        return taskParameters;
    }

    public void setTaskParameters(String taskParameters) {
        this.taskParameters = taskParameters;
    }

    public Instant getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Instant submitTime) {
        this.submitTime = submitTime;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(Instant dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public int getTaskStatus() {
        return taskStatus;
    }

    /*
     * TODO add code to also mark the this.xfTaskflow.statsMightDirty flag
     */
    public void setTaskStatus(int newStatus)
    {
        this.taskStatus = newStatus;
        if (this.getStatusIsFinal())
        {
            if (this.xfTaskflow != null)
            {
                // TODO how to persist this change to DB
                // http://www.baeldung.com/cascading-with-dbref-and-lifecycle-events-in-spring-data-mongodb
                this.xfTaskflow.setStatusPotentialDirty(true);
            }
        }
    }
    public String getTaskStatusFinalReason() {
        return taskStatusFinalReason;
    }

    public void setTaskStatusFinalReason(String taskStatusFinalReason) {
        this.taskStatusFinalReason = taskStatusFinalReason;
    }

    public List<String> getTaskHistory() {
        return taskHistory;
    }

    public void setTaskHistory(List<String> taskHistory) {
        this.taskHistory = taskHistory;
    }

    public Instant getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Instant expireTime) {
        this.expireTime = expireTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDtgId() {
        return dtgId;
    }

    public void setDtgId(String dtgId) {
        this.dtgId = dtgId;
    }

    public String getTriggerId() {
        return TriggerId;
    }

    public void setTriggerId(String triggerId) {
        TriggerId = triggerId;
    }

    public List<String> getAssociatedDtgIds() {
        return associatedDtgIds;
    }

    public void setAssociatedDtgIds(List<String> associatedDtgIds) {
        this.associatedDtgIds = associatedDtgIds;
    }

    public String getCreatedBySiblingTaskId() {
        return createdBySiblingTaskId;
    }

    public void setCreatedBySiblingTaskId(String createdBySiblingTaskId) {
        this.createdBySiblingTaskId = createdBySiblingTaskId;
    }

    public String getMaterializedPathToParent() {
        return materializedPathToParent;
    }

    public void setMaterializedPathToParent(String materializedPathToParent) {
        this.materializedPathToParent = materializedPathToParent;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantDbName() {
        return tenantDbName;
    }

    public void setTenantDbName(String tenantDbName) {
        this.tenantDbName = tenantDbName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainDbName() {
        return domainDbName;
    }

    public void setDomainDbName(String domainDbName) {
        this.domainDbName = domainDbName;
    }

    public String getTaskCallbackQueue() {
        return taskCallbackQueue;
    }

    public void setTaskCallbackQueue(String taskCallbackQueue) {
        this.taskCallbackQueue = taskCallbackQueue;
    }

    public boolean isNeedBroadCallbackToAllApiServer() {
        return needBroadCallbackToAllApiServer;
    }

    public void setNeedBroadCallbackToAllApiServer(boolean needBroadCallbackToAllApiServer) {
        this.needBroadCallbackToAllApiServer = needBroadCallbackToAllApiServer;
    }

    public XFTaskflow getXfTaskflow() {
        return xfTaskflow;
    }

    public void setXfTaskflow(XFTaskflow xfTaskflow) {
        this.xfTaskflow = xfTaskflow;
        if(xfTaskflow != null) {
        	this.taskflowId = xfTaskflow.getId();
        }
        // the XFTaskflow object is supposed to be saved immediately before XFTask is saved
        // so we can safely save it again ?
        this.xfTaskflow.setStatusPotentialDirty(true);
    }

    public boolean isFinalTrigger(){ return finalTrigger; }
    public void setFinalTrigger(boolean finalTrigger){ this.finalTrigger = finalTrigger; }
    public boolean getWaitedByParent() {
        return waitedByParent;
    }

    public void setWaitedByParent(boolean waitedByParent) {
        this.waitedByParent = waitedByParent;
    }

    public int getXFAgentProcessId() {
        return xfAgentProcessId;
    }

    public void setXFAgentProcessId(int XFAgentProcessId) {
        this.xfAgentProcessId = XFAgentProcessId;
    }
    @Indexed
    private String taskflowId = "";

    @Indexed
    private String rootTaskId = "";

    @Indexed
    private String parentTaskId = "";

    @Indexed
    private String jobId = "";

    @Indexed
    private int taskStatus = XFCommon.TASKSTATUS_Scheduled;

    private String taskStatusFinalReason = "";

    private List<String> taskHistory = new ArrayList<String>();

    private String shortDescription = "";

    private String taskType = "";
    private int taskPriority = XFCommon.TASK_PRIORITY_LOW;
    private int taskRabbitmqPriority = XFCommon.TASK_RABBITMQ_PRIORITY_LOW;
    private int taskLevelFromRoot = 0;

    private boolean waitedByParent = true;

    private String userName = "";
    private String userIP = "";
    private String tenantId = "";
    private String tenantDbName = "";
    private String domainId = "";
    private String domainDbName = "";

    private String taskCallbackQueue = "";
    private boolean needBroadCallbackToAllApiServer = false;
    private int workerRestartTimes = 0;
    private int workerRestartTimesUsed = 0;
    private boolean workerIsRestart = false;
    private int workerProcessId = -1;
    private String workerMachineName = "";
    private String jobRunCategory = CATEGORY_OnDemand;
    private String taskParameters = "";
    private Instant submitTime = Instant.now();
    private Instant dispatchTime;
    private Instant startTime;
    private Instant endTime;

    private int xfAgentProcessId = -1;

    private Instant expireTime;
    private String errorMessage = "";
    private String dtgId = "";
    private String TriggerId = "";
    private List<String> associatedDtgIds = new ArrayList<String>();
    private String createdBySiblingTaskId = "";
    private String materializedPathToParent = "";
    private boolean finalTrigger = false;

    @DBRef
    private XFTaskflow xfTaskflow;

    /**
     * a one-way flag indicating if any event has been logged on a task. Once it goes true, never goes back to false.
     * This is not a thread-safe property to avoid performance cost incurred by locking
     */
    @Transient
    private Map<Integer, Boolean> hasBeenLogged = new ConcurrentHashMap<>(5);

    public static final int XFTASK_ACTIONLOG_DEQUEUE_UNKNOWN = 0;
    public static final int XFTASK_ACTIONLOG_ENQUEUE = 1;
    public static final int XFTASK_ACTIONLOG_ACK_TIMEOUT = 2;
    public static final int XFTASK_ACTIONLOG_NO_AVAIL_AGENT = 3;
    public static final int XFTASK_ACTIONLOG_REQUEUE_FINAL_TRIGGERED = 4;
    public static final int XFTASK_ACTIONLOG_REQUEUE = 5;
    public static final int XFTASK_ACTIONLOG_DEQUEUE_ALREADY_DONE = 6;
    public static final int XFTASK_ACTIONLOG_NO_AGENT = 7;
    public static final int XFTASK_ACTIONLOG_SELECTED_AGENT = 8;

    public boolean checkAndMarkLogged(int actionType) {
        if (hasBeenLogged.get(actionType) != null && hasBeenLogged.get(actionType)) {
            return true;
        } else {
            hasBeenLogged.put(actionType, true);
            return false;
        }
    }

    @Override
    public String toString() {
        return ToLogString();
    }
}
