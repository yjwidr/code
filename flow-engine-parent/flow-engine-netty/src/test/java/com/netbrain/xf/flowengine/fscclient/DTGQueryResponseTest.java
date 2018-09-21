package com.netbrain.xf.flowengine.fscclient;

import org.junit.Assert;
import org.junit.Test;

public class DTGQueryResponseTest {

    @Test(expected = Exception.class)
    public void testParseResponseInvalidResponse() throws Exception {
        DTGQueryResponse.parseResponse("Invalid response");
    }

    @Test
    public void testParseResponseEmptyResponse() throws Exception {
        DTGQueryResponse response = DTGQueryResponse.parseResponse("{\"version\":1,\"protocol\":2010,\"test\":\"testdata\",\"bodysize\":84}\r\n\r\n{}");
        Assert.assertTrue(response.getFinishedDtgIDs().isEmpty());
        Assert.assertTrue(response.getNotExistDtgIDs().isEmpty());
    }

    @Test
    public void testParseResponse() throws Exception {
        String header = "{\"version\":1,\"protocol\":2010,\"test\":\"testdata\",\"bodysize\":84}";
        String body = "{\"not_exist\":[\"912fd6bc-fd84-4bc8-9fa9-330757bb44f1\"],\"time\":\"2017-12-18 16:30:28\"}";
        DTGQueryResponse response = DTGQueryResponse.parseResponse(header + "\r\n\r\n" + body);
        Assert.assertTrue(response.getFinishedDtgIDs().isEmpty());
        Assert.assertEquals(1, response.getNotExistDtgIDs().size());
    }
}
