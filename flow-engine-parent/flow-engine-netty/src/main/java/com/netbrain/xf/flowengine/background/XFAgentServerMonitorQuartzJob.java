package com.netbrain.xf.flowengine.background;

import com.netbrain.xf.flowengine.dao.XFAgentRepository;
import com.netbrain.xf.flowengine.dao.XFDtgRepository;
import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.flowengine.utility.CommonUtil;
import com.netbrain.xf.flowengine.daoinmemory.XFAgentInMemoryRepository;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentMetadata;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.xfcommon.XFCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@DisallowConcurrentExecution
public class XFAgentServerMonitorQuartzJob extends QuartzJobBean {

    @Value("${workerserver.servernames}")
    private String workerserverNames;

    @Value("${workerserver.selection.cpu.high.watermark}")
    private long cpuHighWatermark;

    @Value("${workerserver.selection.ram.high.watermark}")
    private long ramHighWatermark;

    @Autowired
    private XFTaskRepository taskRepository;

    @Autowired
    XFAgentInMemoryRepository xfAgentInMemoryRepository;

    @Autowired
    private XFAgentRepository xfAgentRepository;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private DataCenterSwitching dcSwitching;

    private static Logger logger = LogManager.getLogger(XFAgentServerMonitorQuartzJob.class.getSimpleName());

    @Override
    protected void executeInternal(JobExecutionContext context)throws JobExecutionException {
        if(dcSwitching.isActiveDC()) {
            PerformMonitoringAction();
        }
        else{
            logger.debug("Noop in inactive DC.");
        }
    }

    private void PerformMonitoringAction()
    {
        List<XFAgent> listAgentsInDB = this.xfAgentRepository.findAll();
        Map<String,XFAgent> xfagentserverInMemoryHashMap = this.xfAgentInMemoryRepository.GetXfagentserverHashMap();
        for(Map.Entry<String, XFAgent> entry : xfagentserverInMemoryHashMap.entrySet())
        {
            String serverNameInMemory = entry.getKey();
            XFAgent agentInfoInMemroy = entry.getValue();
            String agentInfoInMemoryId = agentInfoInMemroy.getId();

            XFAgent matchedAgentInDB = null;
            // try to find the corresponding XFAgent in DB
            for (XFAgent agentInDB : listAgentsInDB)
            {
                if (agentInDB.getId().equals(agentInfoInMemoryId) && agentInDB.getServerName().equals(serverNameInMemory))
                {
                    matchedAgentInDB = agentInDB;
                    break;
                }
            }

            if (matchedAgentInDB != null)
            {
                commonUtil.ProcessNewlyReceivedXFAgentInformation(agentInfoInMemroy, matchedAgentInDB, XFCommon.XFAgentInformationFrom.FROM_DB_UPDATE);
            } // end of if (matchedAgentInDB != null)
        }// end of for loop
    }
}