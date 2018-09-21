package com.netbrain.xf.flowengine.workerservermanagement;

import com.netbrain.xf.flowengine.fscclient.AMQPTriggerReceiver;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.xfcommon.XFCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XFAgentMetadata {

    private static Logger logger = LogManager.getLogger(XFAgentMetadata.class.getSimpleName());

    public XFAgentMetadata() {
    }

    public String getUniqIdForEachUpdate() {
        return uniqIdForEachUpdate;
    }

    public void setUniqIdForEachUpdate(String uniqIdForEachUpdate) {
        this.uniqIdForEachUpdate = uniqIdForEachUpdate;
    }

    public Instant getFirsttimeReceivedThisUniqId() {
        return firsttimeReceivedThisUniqId;
    }

    public void setFirsttimeReceivedThisUniqId(Instant firsttimeReceivedThisUniqId) {
        this.firsttimeReceivedThisUniqId = firsttimeReceivedThisUniqId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }

    public String getBlacklistedReason() {
        return blacklistedReason;
    }

    public void setBlacklistedReason(String blacklistedReason) {
        this.blacklistedReason = blacklistedReason;
    }

    public int getBlacklistedReasonCode() {
        return blacklistedReasonCode;
    }

    public void setBlacklistedReasonCode(int blacklistedReasonCode) {
        this.blacklistedReasonCode = blacklistedReasonCode;
    }

    public int getRecentTaskCount() {
        return recentTaskCount;
    }

    public void setRecentTaskCount(int recentTaskCount) {
        this.recentTaskCount = recentTaskCount;
    }

    public boolean addOrUpdateOneUnackedXFTask( UnackedXFTaskInfo unackedTask)
    {
        try {
            String xftaskid = unackedTask.getXfTask().getSelfTaskId();
            this.unackedXFTaskHashMap.put(xftaskid, unackedTask);
            return true;
        }
        catch (Exception ex)
        {
            logger.warn("addOrUpdateOneUnackedXFTask exception: ", ex);
        }
        return false;
    }

    public UnackedXFTaskInfo getOneUnackedXFTask(String xftaskid)
    {
        UnackedXFTaskInfo retTask = this.unackedXFTaskHashMap.get(xftaskid);
        return retTask;
    }

    public boolean deleteOneUnackedXFTask(String xftaskid)
    {
        this.unackedXFTaskHashMap.remove(xftaskid);
        return true;
    }

    public boolean clearUnackedXFTask()
    {
        this.unackedXFTaskHashMap.clear();
        return true;
    }

    public int getUnackedXFTaskCount()
    {
        int nSize = this.unackedXFTaskHashMap.size();
        return nSize;
    }

    public long getMissingHeartbeatCount() {
        return missingHeartbeatCount;
    }

    public void setMissingHeartbeatCount(long missingHeartbeatCount) {
        this.missingHeartbeatCount = missingHeartbeatCount;
    }

    public Map<String, UnackedXFTaskInfo> getUnackedXFTaskHashMap()
    {
        return this.unackedXFTaskHashMap;
    }

    private String serverName ="";
    private String uniqIdForEachUpdate = "";
    private Instant firsttimeReceivedThisUniqId;

    private boolean blacklisted = false;
    private String blacklistedReason = "";
    private int blacklistedReasonCode = XFCommon.XFAgentBlacklistedReasonCode.REASON_NOT_BLACKLISTED;
    private int recentTaskCount = 0;
    private long missingHeartbeatCount = 0;

    private Map<String, UnackedXFTaskInfo> unackedXFTaskHashMap = new ConcurrentHashMap<String, UnackedXFTaskInfo>();
}
