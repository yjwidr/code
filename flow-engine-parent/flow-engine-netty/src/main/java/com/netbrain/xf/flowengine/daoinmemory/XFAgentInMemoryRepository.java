package com.netbrain.xf.flowengine.daoinmemory;

import com.netbrain.xf.flowengine.metric.Metrics;
import com.netbrain.xf.flowengine.workerservermanagement.UnackedXFTaskInfo;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentMetadata;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class XFAgentInMemoryRepository {
    private static Logger logger = LogManager.getLogger(XFAgentInMemoryRepository.class.getSimpleName());

    Map<String,XFAgent> xfagentserverHashMap = new ConcurrentHashMap<String,XFAgent>();


    Map<String, XFAgentMetadata> xfagentserverMetadataHashMap = new ConcurrentHashMap<String,XFAgentMetadata>();

    @Autowired
    private Metrics metrics;

    @PostConstruct
    void init()
    {
        metrics.setTaskUnackSnapshot((Long unuse)->{
            long count = 0;
            for (Map.Entry<String, XFAgentMetadata> entry: xfagentserverMetadataHashMap.entrySet()) {
                if (entry.getValue() != null ) {
                    count += entry.getValue().getUnackedXFTaskCount();
                }
            }
            return count;
        });
    }

    public boolean AddOrUpdateOneXFAgent(XFAgent oneServer)
    {
        String serverName = oneServer.getServerName();
        XFAgent xfagent = this.xfagentserverHashMap.get(serverName);
        if(xfagent != null){
            oneServer.setRetired( xfagent.isRetired());
        }
        this.xfagentserverHashMap.put(serverName, oneServer);
        return true;
    }

    /**
     * Only update heartbeat related information. Skip all server resource related fields.
     * @param oneServer
     * @return
     */
    public boolean updateOneXFAgentHeartbeatInfoOnly(XFAgent oneServer)
    {
        String serverName = oneServer.getServerName();
        XFAgent xfAgent = xfagentserverHashMap.get(serverName);
        xfAgent.setXfAgentProcessId(oneServer.getXfAgentProcessId());
        xfAgent.setUniqIdForEachUpdate(oneServer.getUniqIdForEachUpdate());
        xfAgent.setXfAgentServerTimestampUTC(oneServer.getXfAgentServerTimestampUTC());
        return true;
    }

    public boolean DeleteOneXFAgent(String strServerName)
    {
        this.xfagentserverHashMap.remove(strServerName);
        return true;
    }

    public XFAgent GetOneXFAgent(String strServerName)
    {
        XFAgent retObj = this.xfagentserverHashMap.get(strServerName);
        return retObj;
    }

    public Map<String, XFAgent> getXFAgentServerHashMap() {
        return xfagentserverHashMap;
    }

    public List<XFAgent> GetAllInMemoryXFAgent()
    {
        List<XFAgent> listXFAgentInMem = new ArrayList<XFAgent>();
        for(Map.Entry<String, XFAgent> entry : xfagentserverHashMap.entrySet()) {
            XFAgent oneAgentInMemroy = entry.getValue();
            listXFAgentInMem.add(oneAgentInMemroy);
        }
        return listXFAgentInMem;
    }

    public boolean AddOrUpdateOneXFAgentMetadata(XFAgentMetadata oneServerMetadata)
    {
        String serverName = oneServerMetadata.getServerName();
        this.xfagentserverMetadataHashMap.put(serverName, oneServerMetadata);
        return true;
    }

    public boolean DeleteOneXFAgentMetadata(String strServerName)
    {
        this.xfagentserverMetadataHashMap.remove(strServerName);
        return true;
    }

    public XFAgentMetadata GetOneXFAgentMetadata(String strServerName)
    {
        XFAgentMetadata retObj = this.xfagentserverMetadataHashMap.get(strServerName);
        return retObj;
    }

    public Map<String, XFAgent> GetXfagentserverHashMap() {
        return xfagentserverHashMap;
    }

    public void SetXfagentserverHashMap(Map<String, XFAgent> xfagentserverHashMap) {
        this.xfagentserverHashMap = xfagentserverHashMap;
    }


    public Map<String, XFAgentMetadata> getXfagentserverMetadataHashMap() {
        return xfagentserverMetadataHashMap;
    }

    public void setXfagentserverMetadataHashMap(Map<String, XFAgentMetadata> xfagentserverMetadataHashMap) {
        this.xfagentserverMetadataHashMap = xfagentserverMetadataHashMap;
    }

    private boolean addOrUpdateOneUnackedXFTask(String strServerName, XFTask unackedTask)
    {
        try {
            XFAgentMetadata agentMetadata = this.xfagentserverMetadataHashMap.get(strServerName);
            if (agentMetadata == null) {
                return false;
            }
            Instant timeNow = Instant.now();
            int resentTimes = 0;
            String taskId = unackedTask.getSelfTaskId();
            UnackedXFTaskInfo unackedXFTaskInfo = agentMetadata.getOneUnackedXFTask(taskId);
            if (unackedXFTaskInfo != null) {
                resentTimes = unackedXFTaskInfo.getResendTimes() + 1;
            }

            unackedXFTaskInfo = new UnackedXFTaskInfo(unackedTask, timeNow, resentTimes);
            agentMetadata.addOrUpdateOneUnackedXFTask(unackedXFTaskInfo);

            return true;
        }
        catch (Exception ex)
        {
            logger.warn("addOrUpdateOneUnackedXFTaskInfo exception: ", ex);
        }

        return false;
    }

    public boolean addOrUpdateOneUnackedXFTaskInfo(String strServerName, UnackedXFTaskInfo unackedXFTaskInfo)
    {
        try {
            XFAgentMetadata agentMetadata = this.xfagentserverMetadataHashMap.get(strServerName);
            if (agentMetadata == null) {
                return false;
            }
            agentMetadata.addOrUpdateOneUnackedXFTask(unackedXFTaskInfo);

            return true;
        }
        catch (Exception ex)
        {
            logger.warn("addOrUpdateOneUnackedXFTaskInfo exception: ", ex);
        }

        return false;
    }

    public boolean deleteOneUnackedXFTaskInfo(String strServerName, String xftaskId)
    {
        try {
            if (StringUtils.isEmpty(strServerName))
            {
                // When no servername is given, go through all servers and remove the task from the unack queue
                // This should NOT happen unless XFAgent is older than Jan 25, 2018
                for (Map.Entry<String, XFAgentMetadata> entry: xfagentserverMetadataHashMap.entrySet()) {
                    if (entry.getValue() != null ) {
                        entry.getValue().deleteOneUnackedXFTask(xftaskId);
                    }
                }
                return false;
            }
            XFAgentMetadata agentMetadata = this.xfagentserverMetadataHashMap.get(strServerName);
            if (agentMetadata == null) {

                return false;
            }

            agentMetadata.deleteOneUnackedXFTask(xftaskId);
        }
        catch (Exception ex)
        {
            logger.warn("deleteOneUnackedXFTaskInfo({} {}) exception: ", strServerName, xftaskId, ex);
        }

        return false;
    }
}
