package com.netbrain.xf.model;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

public class XFTaskflowTest {
    @Test
    public void testToString() throws Exception {
        XFTaskflow xfTaskflow = new XFTaskflow();
        xfTaskflow.setId("123-321");
        xfTaskflow.setSubmitTime(Instant.ofEpochSecond(30*365*24*3600));
        Assert.assertEquals("XFTaskflow[id=123-321, status=1, submitted at 1999-12-25T00:00:00Z, start time null, end time null]",
                xfTaskflow.toString());
        Assert.assertEquals("123-321", xfTaskflow.getId());
    }
}
