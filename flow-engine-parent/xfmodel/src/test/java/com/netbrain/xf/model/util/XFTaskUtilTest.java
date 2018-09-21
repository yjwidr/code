package com.netbrain.xf.model.util;

import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.xfcommon.XFCommon;
import org.junit.Assert;
import org.junit.Test;

public class XFTaskUtilTest {
    @Test
    public void testIsBeforeRunning() throws Exception {
        Assert.assertTrue(new XFTaskUtil().isBeforeRunning(null));

        XFTask task = new XFTask();
        Assert.assertTrue(new XFTaskUtil().isBeforeRunning(task));

        task.setTaskStatus(XFCommon.TASKSTATUS_Running);
        Assert.assertFalse(new XFTaskUtil().isBeforeRunning(task));

        task.setTaskStatus(XFCommon.TASKSTATUS_Canceled);
        Assert.assertFalse(new XFTaskUtil().isBeforeRunning(task));
    }
}
