package com.netbrain.xf.flowengine.workerservermanagement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XFTaskSummary {

    public String selfTaskId;
    public int taskStatusAsInt;
    public String taskStatusFinalReason = "";
}
