package com.netbrain.xf.flowengine.workerservermanagement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.netbrain.xf.model.XFAgent;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class XFAgentHeartBeatMessage {

    public XFAgent xfAgentInfo;

    public HashMap<String, XFTaskSummary> selfTaskId2XFTaskSummaryDict;
}
