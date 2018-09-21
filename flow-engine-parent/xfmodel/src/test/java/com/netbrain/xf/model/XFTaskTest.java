package com.netbrain.xf.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class XFTaskTest {

    @Test
    public void testDtgId() throws Exception {
        XFTask xfTask = new XFTask();

        String aUuid = UUID.randomUUID().toString();
        xfTask.setDtgId(aUuid);
        Assert.assertEquals(aUuid, xfTask.getDtgId());
    }

    @Test
    public void testAssociatedDtgIds() throws Exception {
        XFTask xfTask = new XFTask();

        String aUuid = UUID.randomUUID().toString();
        List<String> listIds = new ArrayList<String>();
        listIds.add(aUuid);
        xfTask.setAssociatedDtgIds(listIds);
        List<String> retlistIds = xfTask.getAssociatedDtgIds();

        String bUuid = retlistIds.get(0);
        Assert.assertEquals(bUuid, aUuid);
    }

    public void test_isRootTask() throws Exception {
        XFTask xfTask = new XFTask();
        Assert.assertEquals(false, xfTask.isRootTask());

        String rootTaskId = UUID.randomUUID().toString();
        String parentTaskId = UUID.randomUUID().toString();
        String selTaskId = UUID.randomUUID().toString();

        xfTask.setRootTaskId(rootTaskId);
        xfTask.setParentTaskId(parentTaskId);
        xfTask.setId(selTaskId);
        Assert.assertEquals(false, xfTask.isRootTask());


        xfTask.setRootTaskId(rootTaskId);
        xfTask.setParentTaskId("");
        xfTask.setId(rootTaskId);
        Assert.assertEquals(true, xfTask.isRootTask());

    }


}
