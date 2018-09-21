package com.netbrain.xf.flowengine.dao;

import com.netbrain.xf.model.XFTask;

import java.time.Instant;
import java.util.List;

public interface XFTaskRepositoryCustom {

    boolean updateStatus(XFTask origTask, int newStatus, String strTaskStatusFinalReason, Instant timestamp);

    boolean updateWorkerServerName(XFTask origTask, String newServerName);

    boolean updateFinalTrigger(XFTask xfTask, boolean finalTrigger);

    boolean stopXFTasksByTaskflowId(String taskflowId, String strReason);

    boolean updateTaskWhenXFAgentProcessCrashed(String xfagentServerName, int crashedXFAgentProcessId);

    boolean updateTaskWhenXFAgentProcessNoUpdateTooLong(String xfagentServerName);

    List<XFTask> findAllUnfinishedTasks();

    List<XFTask> findTopNUnfinishedTasksByTaskPriorityAndSubmitTime(int nLimit);
}
