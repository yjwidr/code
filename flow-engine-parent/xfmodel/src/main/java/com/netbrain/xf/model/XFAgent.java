package com.netbrain.xf.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbrain.xf.xfcommon.XFCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "XFAgent")
public class XFAgent {

    private static Logger logger = LogManager.getLogger(XFAgent.class.getSimpleName());

    public XFAgent() {
    }

    // For our design, the id is also the serverName. This is an easy way to gain uniqueness of serveName.
    @Id
    private String id;

    @Indexed
    private String serverName;
    private int xfAgentProcessId = -1;
    private String xfAgentRunningInstanceId = "";
    private String uniqIdForEachUpdate = "";
    private Instant xfAgentServerTimestampUTC;
    private int updateIntervalInSeconds = XFCommon.DefaultAndGuessedXFAgentHeartBeatIntervalInSeconds;
    private boolean calculatedResourceUsage = true;
    private long serverCpuPercent = XFCommon.UNKNOWN_CPU;
    private long serverPhysicalTotalMemoryInByte = XFCommon.UNKNOWN_PhysicalTotalMemoryInByte;
    private long serverPhysicalAvailableMemoryInByte = XFCommon.UNKNOWN_PhysicalAvailableMemoryInByte;
    private boolean serverIsOverloaded = true; // default is true, so a newly joining XFAgent server will not be selected until the XFAgnet updates the DB.
    private boolean p1IsOverloaded = true; // default is true, so a newly joining XFAgent server will not be selected until the XFAgnet updates the DB.
    private boolean p2IsOverloaded = true; // default is true, so a newly joining XFAgent server will not be selected until the XFAgnet updates the DB.
    private boolean p3IsOverloaded = true; // default is true, so a newly joining XFAgent server will not be selected until the XFAgnet updates the DB.
    private long serverVirtualMemoryTotalAllowedInByte = 0;
    private long serverVirtualMemoryTotalUsedByAllPriorityWorkersInByte = Long.MAX_VALUE/2; // large enough not to be an actual number on a server.
    private long p1BucketAllowedBytePercent = 0;
    private long p1BucketAllowedByte = 0;
    private long p1ActualUsedByte = 0;
    private long p1AvailableByte = 0;
    private long p2BucketAllowedBytePercent = 0;
    private long p2BucketAllowedByte = 0;
    private long p2ActualUsedByte = 0;
    private long p2AvailableByte = 0;
    private long p3BucketAllowedBytePercent = 0;
    private long p3BucketAllowedByte = 0;



    private long p3ActualUsedByte = 0;
    private long p3AvailableByte = 0;

    @Transient
    private boolean retired = false;

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getServerName() {
        return serverName;
    }
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    public int getXfAgentProcessId() {
        return xfAgentProcessId;
    }

    public void setXfAgentProcessId(int processIDXFAgent) {
        this.xfAgentProcessId = processIDXFAgent;
    }

    public String getXfAgentRunningInstanceId() {
        return xfAgentRunningInstanceId;
    }

    public void setXfAgentRunningInstanceId(String xfAgentRunningInstanceId) {
        this.xfAgentRunningInstanceId = xfAgentRunningInstanceId;
    }

    public long getServerCpuPercent() {
        return serverCpuPercent;
    }

    public void setServerCpuPercent(long serverCpuPercent) {
        this.serverCpuPercent = serverCpuPercent;
    }

    public String getUniqIdForEachUpdate() {
        return uniqIdForEachUpdate;
    }

    public void setUniqIdForEachUpdate(String uniqIdForEachUpdate) {
        this.uniqIdForEachUpdate = uniqIdForEachUpdate;
    }
    public int getUpdateIntervalInSeconds() {
        return updateIntervalInSeconds;
    }

    public Instant getXfAgentServerTimestampUTC() {
        return xfAgentServerTimestampUTC;
    }

    public void setXfAgentServerTimestampUTC(Instant xfAgentServerTimestampUTC) {
        this.xfAgentServerTimestampUTC = xfAgentServerTimestampUTC;
    }

    public void setUpdateIntervalInSeconds(int updateIntervalInSeconds) {
        this.updateIntervalInSeconds = updateIntervalInSeconds;
    }

    public boolean isCalculatedResourceUsage() {
        return calculatedResourceUsage;
    }

    public void setCalculatedResourceUsage(boolean calculatedResourceUsage) {
        this.calculatedResourceUsage = calculatedResourceUsage;
    }

    public long getServerPhysicalTotalMemoryInByte() {
        return serverPhysicalTotalMemoryInByte;
    }

    public void setServerPhysicalTotalMemoryInByte(long serverPhysicalTotalMemoryInByte) {
        this.serverPhysicalTotalMemoryInByte = serverPhysicalTotalMemoryInByte;
    }

    public long getServerPhysicalAvailableMemoryInByte() {
        return serverPhysicalAvailableMemoryInByte;
    }

    public void setServerPhysicalAvailableMemoryInByte(long serverPhysicalAvailableMemoryInByte) {
        this.serverPhysicalAvailableMemoryInByte = serverPhysicalAvailableMemoryInByte;
    }

    public boolean isServerIsOverloaded() {
        return serverIsOverloaded;
    }
    public void setServerIsOverloaded(boolean serverIsOverloaded) {
        this.serverIsOverloaded = serverIsOverloaded;
    }
    public boolean isP1IsOverloaded() {
        return p1IsOverloaded;
    }
    public void setP1IsOverloaded(boolean p1IsOverloaded) {
        this.p1IsOverloaded = p1IsOverloaded;
    }
    public boolean isP2IsOverloaded() {
        return p2IsOverloaded;
    }
    public void setP2IsOverloaded(boolean p2IsOverloaded) {
        this.p2IsOverloaded = p2IsOverloaded;
    }
    public boolean isP3IsOverloaded() {
        return p3IsOverloaded;
    }
    public void setP3IsOverloaded(boolean p3IsOverloaded) {
        this.p3IsOverloaded = p3IsOverloaded;
    }
    public long getServerVirtualMemoryTotalAllowedInByte() {
        return serverVirtualMemoryTotalAllowedInByte;
    }
    public void setServerVirtualMemoryTotalAllowedInByte(long serverVirtualMemoryTotalAllowedInByte) {
        this.serverVirtualMemoryTotalAllowedInByte = serverVirtualMemoryTotalAllowedInByte;
    }

    public long getServerVirtualMemoryTotalUsedByAllPriorityWorkersInByte() {
        return serverVirtualMemoryTotalUsedByAllPriorityWorkersInByte;
    }

    public void setServerVirtualMemoryTotalUsedByAllPriorityWorkersInByte(long serverVirtualMemoryTotalUsedByAllPriorityWorkersInByte) {
        this.serverVirtualMemoryTotalUsedByAllPriorityWorkersInByte = serverVirtualMemoryTotalUsedByAllPriorityWorkersInByte;
    }


    public long getP1BucketAllowedBytePercent() {
        return p1BucketAllowedBytePercent;
    }

    public void setP1BucketAllowedBytePercent(long p1BucketAllowedBytePercent) {
        this.p1BucketAllowedBytePercent = p1BucketAllowedBytePercent;
    }

    public long getP1BucketAllowedByte() {
        return p1BucketAllowedByte;
    }

    public void setP1BucketAllowedByte(long p1BucketAllowedByte) {
        this.p1BucketAllowedByte = p1BucketAllowedByte;
    }

    public long getP2BucketAllowedBytePercent() {
        return p2BucketAllowedBytePercent;
    }

    public void setP2BucketAllowedBytePercent(long p2BucketAllowedBytePercent) {
        this.p2BucketAllowedBytePercent = p2BucketAllowedBytePercent;
    }

    public long getP2BucketAllowedByte() {
        return p2BucketAllowedByte;
    }

    public void setP2BucketAllowedByte(long p2BucketAllowedByte) {
        this.p2BucketAllowedByte = p2BucketAllowedByte;
    }

    public long getP3BucketAllowedBytePercent() {
        return p3BucketAllowedBytePercent;
    }

    public void setP3BucketAllowedBytePercent(long p3BucketAllowedBytePercent) {
        this.p3BucketAllowedBytePercent = p3BucketAllowedBytePercent;
    }

    public long getP3BucketAllowedByte() {
        return p3BucketAllowedByte;
    }

    public void setP3BucketAllowedByte(long p3BucketAllowedByte) {
        this.p3BucketAllowedByte = p3BucketAllowedByte;
    }

    public long getP1ActualUsedByte() {
        return p1ActualUsedByte;
    }

    public void setP1ActualUsedByte(long p1ActualUsedByte) {
        this.p1ActualUsedByte = p1ActualUsedByte;
    }

    public long getP2ActualUsedByte() {
        return p2ActualUsedByte;
    }

    public void setP2ActualUsedByte(long p2ActualUsedByte) {
        this.p2ActualUsedByte = p2ActualUsedByte;
    }

    public long getP3ActualUsedByte() {
        return p3ActualUsedByte;
    }

    public void setP3ActualUsedByte(long p3ActualUsedByte) {
        this.p3ActualUsedByte = p3ActualUsedByte;
    }
    public long getP1AvailableByte() {
        return p1AvailableByte;
    }

    public void setP1AvailableByte(long p1AvailableByte) {
        this.p1AvailableByte = p1AvailableByte;
    }

    public long getP2AvailableByte() {
        return p2AvailableByte;
    }

    public void setP2AvailableByte(long p2AvailableByte) {
        this.p2AvailableByte = p2AvailableByte;
    }

    public long getP3AvailableByte() {
        return p3AvailableByte;
    }

    public void setP3AvailableByte(long p3AvailableByte) {
        this.p3AvailableByte = p3AvailableByte;
    }

    @Override
    public String toString() {
        return String.format("XFAgent[id=%s,serverName=%s]", id, serverName);
    }


    static public XFAgent convertJsonStr2XFAgentObject(String strJson)
    {
        ObjectMapper mapper = new ObjectMapper();
        XFAgent retXFAgent = null;
        try {
            retXFAgent = mapper.readValue(strJson, XFAgent.class);
        }
        catch (IOException ioe)
        {
            logger.warn("failed to convert to XFAgent object from json string :" + strJson);
            retXFAgent = null;
        }

        return retXFAgent;
    }

    static public String convertXFagentobjectToJsonStr(XFAgent xfagentObj)
    {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "";
        try {
            //Object to JSON in String
            jsonInString = mapper.writeValueAsString(xfagentObj);
        }
        catch (Exception e)
        {
            logger.warn("failed to convert XFAgent object to json string." + xfagentObj.toString(), e);
            jsonInString = "";
        }
        return jsonInString;
    }

}
