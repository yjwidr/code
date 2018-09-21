package com.netbrain.xf.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import com.netbrain.xf.xfcommon.XFCommon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class XFAgentTest {

    private Car convertJsonStr2CarObject(String strJson)
    {
        ObjectMapper mapper = new ObjectMapper();
        Car retPojo = null;
        try {
            retPojo = mapper.readValue(strJson, Car.class);
        }
        catch (IOException ioe)
        {
            retPojo = null;
        }

        return retPojo;
    }


    @Test
    public void test_json2POJO() throws Exception {

        String strJsonBad = "\"ID\":\"af469a01-9894-4c8a-a618-99fc2baa1cb3\"}";
        String strJson = "{\"id\":\"af469a01-9894-4c8a-a618-99fc2baa1cb3\"}";
        String json = "{ \"color\" : \"Black\", \"type\" : \"BMW\" }";
        String xfagentJson = "{\"ID\":\"af469a01-9894-4c8a-a618-99fc2baa1cb3\",\"ServerName\":\"NB-DTP-269\",\"XFAgentProcessId\":54308,\"UniqIdForEachUpdate\":\"22855885-af6d-4899-98ee-84c39c639b2d\",\"UpdateIntervalInSeconds\":20,\"ServerCpuPercent\":10,\"ServerPhysicalTotalMemoryInByte\":34260262912,\"ServerPhysicalAvailableMemoryInByte\":6529990656,\"ServerIsOverloaded\":false,\"p1IsOverloaded\":false,\"p2IsOverloaded\":false,\"p3IsOverloaded\":false,\"ServerVirtualMemoryTotalAllowedInByte\":42825328640,\"ServerVirtualMemoryTotalUsedByAllPriorityWorkersInByte\":0,\"p1BucketAllowedBytePercent\":20,\"p1BucketAllowedByte\":8565065728,\"p1ActualUsedByte\":0,\"p1AvailableByte\":42825328640,\"p2BucketAllowedBytePercent\":30,\"p2BucketAllowedByte\":12847598592,\"p2ActualUsedByte\":0,\"p2AvailableByte\":34260262912,\"p3BucketAllowedBytePercent\":50,\"p3BucketAllowedByte\":21412664320,\"p3ActualUsedByte\":0,\"p3AvailableByte\":21412664320}";

        ObjectMapper objectMapper = new ObjectMapper();
        //Car car = new Car("yellow", "renault");

        //String carAsString = objectMapper.writeValueAsString(car);


        XFAgent retcar = XFAgent.convertJsonStr2XFAgentObject(xfagentJson);

        Assert.assertNotNull(retcar);


    }
}
@JsonIgnoreProperties(ignoreUnknown = true)
class Car
{
    public Car()
    {

    }


    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String color;
    private String type;
}