package com.netbrain.xf.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class XFDtgTest {
    @Test
    public void testSetId() throws Exception {
        XFDtg xfTask = new XFDtg();
        xfTask.setId("123-321");
        Assert.assertEquals("123-321", xfTask.getId());

    }
    @Test
    public void testAncestorDtgIds() throws Exception {
        XFDtg xfDtg = new XFDtg();
        List<String> ancestorDtgIds = new ArrayList<String>();
        String aUuid = UUID.randomUUID().toString();
        ancestorDtgIds.add(aUuid);
        xfDtg.setAncestorDtgIds(ancestorDtgIds);
        Assert.assertEquals(1, xfDtg.getAncestorDtgIds().size());
        Assert.assertEquals(aUuid, xfDtg.getAncestorDtgIds().get(0));

    }

}
