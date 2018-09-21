package com.netbrain.xf.model;
import com.netbrain.xf.xfcommon.XFCommon;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "XFDtg")
public class XFDtg {


    @Id
    private String id;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getTriggeredTaskType() {
        return triggeredTaskType;
    }

    public void setTriggeredTaskType(String triggeredTaskType) {
        this.triggeredTaskType = triggeredTaskType;
    }

    public String getTriggeredTaskParameters() {
        return triggeredTaskParameters;
    }

    public void setTriggeredTaskParameters(String triggeredTaskParameters) {
        this.triggeredTaskParameters = triggeredTaskParameters;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getRegisteredByTaskId() {
        return registeredByTaskId;
    }

    public void setRegisteredByTaskId(String registeredByTaskId) {
        this.registeredByTaskId = registeredByTaskId;
    }

    public List<String> getAncestorDtgIds() {
        return ancestorDtgIds;
    }

    public void setAncestorDtgIds(List<String> ancestorDtgIds) {
        this.ancestorDtgIds = ancestorDtgIds;
    }

    public boolean isAnyTriggerReceived() {
        return isAnyTriggerReceived;
    }

    public void setAnyTriggerReceived(boolean anyTriggerReceived) {
        isAnyTriggerReceived = anyTriggerReceived;
    }



    public int getTriggerReceivedTotalTimes() {
        return triggerReceivedTotalTimes;
    }

    public void setTriggerReceivedTotalTimes(int triggerReceivedTotalTimes) {
        this.triggerReceivedTotalTimes = triggerReceivedTotalTimes;
    }

    public Instant getFirstTriggerReceivedTime() {
        return firstTriggerReceivedTime;
    }

    public void setFirstTriggerReceivedTime(Instant firstTriggerReceivedTime) {
        this.firstTriggerReceivedTime = firstTriggerReceivedTime;
    }

    public Instant getLastTriggerReceivedTime() {
        return lastTriggerReceivedTime;
    }

    public void setLastTriggerReceivedTime(Instant lastTriggerReceivedTime) {
        this.lastTriggerReceivedTime = lastTriggerReceivedTime;
    }

    public boolean isFinalTriggerReceived() {
        return isFinalTriggerReceived;
    }

    public void setFinalTriggerReceived(boolean finalTriggerReceived) {
        isFinalTriggerReceived = finalTriggerReceived;
    }

    public String getTaskflowId() {
        return taskflowId;
    }

    public void setTaskflowId(String taskflowId) {
        this.taskflowId = taskflowId;
    }
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public int getDtgStatus() {
        return dtgStatus;
    }

    public void setDtgStatus(int dtgStatus) {
        this.dtgStatus = dtgStatus;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Instant submitTime) {
        this.submitTime = submitTime;
    }

    public Instant getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(Instant triggerTime) {
        this.triggerTime = triggerTime;
    }

    public boolean hasFinished() {
        return (dtgStatus >= XFCommon.DTGSTATUS_CompletedNormally);
    }

    @Indexed
    private String taskflowId = "";

    @Indexed
    private String jobId = "";

    @Indexed
    private int dtgStatus = XFCommon.DTGSTATUS_Running;

    private String triggeredTaskType = "";
    private String triggeredTaskParameters = "";
    private String refId = "";
    private String registeredByTaskId = "";
    private List<String> ancestorDtgIds = new ArrayList<String>();
    private boolean isAnyTriggerReceived = false;

    private int triggerReceivedTotalTimes = 0;
    private Instant firstTriggerReceivedTime;
    private Instant lastTriggerReceivedTime;
    private boolean isFinalTriggerReceived = false;
    private String tenantId = "";

    private Instant submitTime;
    // the time when the last trigger is received
    private Instant triggerTime;

    public XFDtg() {
    }

    @Override
    public String toString() {
        return String.format("XFDtg[id=%s, jobId=%s, taskflowId=%s, registeredByTaskId=%s]",
                id,
                jobId,
                taskflowId,
                registeredByTaskId
        );
    }
}
