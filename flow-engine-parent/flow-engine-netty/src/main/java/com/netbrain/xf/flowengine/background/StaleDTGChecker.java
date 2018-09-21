package com.netbrain.xf.flowengine.background;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbrain.ngsystem.model.FSCInfo;
import com.netbrain.ngsystem.model.FrontServerController;
import com.netbrain.ngsystem.model.NoActiveFSCException;
import com.netbrain.xf.flowengine.dao.XFDtgRepository;
import com.netbrain.xf.flowengine.fscclient.*;
import com.netbrain.xf.flowengine.metric.Metrics;
import com.netbrain.xf.flowengine.utility.DataCenterSwitching;
import com.netbrain.xf.flowengine.utility.HASupport;
import com.netbrain.xf.model.XFDtg;
import com.netbrain.xf.xfcommon.XFCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A background job regularly checks if some old DTGs are still valid.
 * Some DTGs may be finalized and we lost the final trigger.
 * Some DTGs may be invalid due to Front Server Controller changes.
 */
@Component
@DisallowConcurrentExecution
public class StaleDTGChecker extends QuartzJobBean {
    private static Logger logger = LogManager.getLogger(StaleDTGChecker.class.getSimpleName());

    @Value("${background.staledtgchecker.stale.age}")
    private int dtgStaleAge;

    @Value("${background.staledtgchecker.batch.size}")
    private int dtgStaleCheckSize;

    @Value(("${background.staledtgchecker.job.enabled}"))
    private boolean jobEnabled;

    @Autowired
    XFDtgRepository dtgRepository;

    @Autowired
    FSCRepository fscRepository;

    @Autowired
    AMQPTriggerReceiver triggerReceiver;
    
    @Autowired
    private HASupport haSupport;

    @Autowired
    private DataCenterSwitching dcSwitching;

    @Autowired
    private Metrics metrics;

    protected void updateDTGStatus(XFDtg staleDtg) {
        FrontServerController fsc = fscRepository.findFSCByTenantId(staleDtg.getTenantId());
        if (fsc != null) {
            NetlibClient netlibClient = new NetlibClient(fsc, metrics);
            try {
                String response = netlibClient.sendCommand(FSCRequest.CMD_fsTaskgroupStatusReq, staleDtg.getId());
                DTGQueryResponse dtgQueryResponse = DTGQueryResponse.parseResponse(response);
                for (String dtgId: dtgQueryResponse.getNotExistDtgIDs()) {
                    try{
                        FSCInfo activeFSCInfo = fsc.getActiveFSCInfo();
                        logger.warn("DTG {} is not found from FSC {}", staleDtg.getId(), activeFSCInfo.getIpOrHostname());
                    }
                    catch(NoActiveFSCException fe){
                        logger.warn("DTG {} is not found from FSC ", staleDtg.getId());
                    }
                    // TODO: should we ask FSC to stop this DTG ?
                    staleDtg.setDtgStatus(XFCommon.DTGSTATUS_NonExist);
                    dtgRepository.save(staleDtg);
                    triggerReceiver.generateAndSubmitTask("FlowEngine", staleDtg.getId(), true);
                }

                for (String dtgId: dtgQueryResponse.getFinishedDtgIDs()) {
                    logger.info("DTG {} has finished but we did not know, processing final trigger", staleDtg.getId());
                    triggerReceiver.generateAndSubmitTask("FlowEngine", staleDtg.getId(), true);
                }
            } catch (Exception e) {
                logger.warn("Failed to query DTG for ID " + staleDtg.getId(), e);
            }
        }
    }

    public void checkDTGLastUpdatedBefore(int lastTriggerElapsed) {
        Instant expirationTime = Instant.now().minusSeconds(lastTriggerElapsed);
        List<XFDtg> staleDtgsNeverTriggered = dtgRepository.findRunningDtgsSubmittedBefore(expirationTime, new PageRequest(0, dtgStaleCheckSize));
        List<XFDtg> staleDtgs = dtgRepository.findRunningDtgsTriggeredBefore(expirationTime, new PageRequest(0, dtgStaleCheckSize));

        for (XFDtg staleDtg: staleDtgsNeverTriggered) {
            updateDTGStatus(staleDtg);
        }

        for (XFDtg staleDtg: staleDtgs) {
            updateDTGStatus(staleDtg);
        }
    }

    public void checkAllRunningDTGs() {
        List<FrontServerController> fscs = fscRepository.findAll(dtgStaleCheckSize);
        for (FrontServerController fsc: fscs) {
            NetlibClient netlibClient = new NetlibClient(fsc, metrics);
            try {
                String runningDTGresponse = netlibClient.sendCommand(FSCRequest.CMD_fsRunningTaskReq, "");
                logger.debug("Running DTGs from FSC: " + runningDTGresponse);
                DTGQueryResponse dtgQueryResponse = DTGQueryResponse.parseResponse(runningDTGresponse);
                for (String dtgId: dtgQueryResponse.getDtgIDs()) {
                    // for each running DTG, stop it if it is already finished in Task Engine database
                    if (dtgId != null) {
                        Optional<XFDtg> dtgOptional = dtgRepository.findById(dtgId);
                        if (dtgOptional.isPresent() && dtgOptional.get().hasFinished()) {
                            logger.info("Running DTGs {} from FSC while it is marked as finished in Task Engine db, send STOP to FSC", dtgId);
                            try {
                                NetlibClient netlibClientStopSender = new NetlibClient(fsc, metrics);
                                netlibClientStopSender.sendCommand(FSCRequest.CMD_fsStopDTGReq, dtgId);
                            } catch (Exception e) {
                                logger.warn("Failed to stop DTG for dtg id " + dtgId, e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                try{
                    FSCInfo activeFSCInfo = fsc.getActiveFSCInfo();
                    logger.error("Failed to query running DTGs from FSC " + activeFSCInfo.getIpOrHostname(), e);
                }
                catch(NoActiveFSCException fe){
                    logger.error("Failed to query running DTGs from FSC ", e);
                }
            }
        }
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (jobEnabled && haSupport.isActive() && dcSwitching.isActiveDC()) {
            logger.debug("Running StaleDTGChecker job");
            checkDTGLastUpdatedBefore(dtgStaleAge);
            checkAllRunningDTGs();
        }
        else{
            logger.debug("Stale DTG checker is disabled or noop in standby mode or inactive DC.");
        }
    }
}
