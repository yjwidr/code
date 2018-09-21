package com.netbrain.xf.model.util;

import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.xfcommon.XFCommon;

public class XFTaskUtil {
    public boolean isBeforeRunning(XFTask task) {
        return (task == null || task.getTaskStatus() < XFCommon.TASKSTATUS_Running);
    }
}
