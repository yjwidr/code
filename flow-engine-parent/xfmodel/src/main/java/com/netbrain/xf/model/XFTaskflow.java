package com.netbrain.xf.model;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Document(collection = "XFTaskflow")
public class XFTaskflow {
    public static final int STATUS_NOT_EXIST = -1;

    public static final int STATUS_Unknown = 0;
    public static final int STATUS_Scheduled = 1;
    public static final int STATUS_Started = 2;
    public static final int STATUS_Running = 3;
    public static final int STATUS_CompletedNormally = 4;
    public static final int STATUS_CompletedWithException = 5;
    public static final int STATUS_CompletedCrash = 6;
    public static final int STATUS_Canceled = 7;

    public List getUnfinishedStates() {
        List<Integer> states = new ArrayList<>();
        states.add(XFTaskflow.STATUS_Scheduled);
        states.add(XFTaskflow.STATUS_Running);
        states.add(XFTaskflow.STATUS_Started);
        return states;
    }

    public boolean getStatusIsFinal()
    {
        return (this.status > STATUS_Running);
    }

    private String id;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Indexed
    private int status = STATUS_Scheduled;
    private boolean statusPotentialDirty = true; // force it to be recalcuated for the firs time
    public Instant getStatusUpdatedTime() {
        return statusUpdatedTime;
    }

    private Instant statusUpdatedTime = Instant.now();

    public int getStatus() {
        return status;
    }

    public void setStatus(int status)
    {
        this.statusUpdatedTime = Instant.now();
        this.status = status;

        // when the status is explicitly updated, set the dirty flag to false?
        this.statusPotentialDirty = false;
    }
    public boolean getStatusPotentialDirty() {
        return statusPotentialDirty;
    }

    public void setStatusPotentialDirty(boolean statusMaybeDirty) {
        this.statusPotentialDirty = statusMaybeDirty;
    }

    private Instant submitTime = Instant.now();
    private Instant startTime;
    private Instant endTime;

    private boolean isStopRequested = false;

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

    public boolean isStopRequested() {
        return isStopRequested;
    }

    public void setStopRequested(boolean stopRequested) {
        isStopRequested = stopRequested;
    }

    @Indexed
    private String jobId;

    /**
     * Job ID is the ID of a scheduled Job.
     * It is NOT the ID of a NGSystem.JobDef document, but the ID of NGSystem.JobDef.job.jobId
     */
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public XFTaskflow() {
    }

    @Override
    public String toString() {
        return String.format("XFTaskflow[id=%s, status=%d, submitted at %s, start time %s, end time %s]",
                id,
                status,
                DateTimeFormatter.ISO_INSTANT.format(submitTime),
                (startTime == null) ? "null" : DateTimeFormatter.ISO_INSTANT.format(startTime),
                (endTime == null) ? "null" : DateTimeFormatter.ISO_INSTANT.format(endTime));
    }
}
