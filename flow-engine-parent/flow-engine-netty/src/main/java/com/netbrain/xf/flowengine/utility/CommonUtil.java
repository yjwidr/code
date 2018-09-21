package com.netbrain.xf.flowengine.utility;

import static com.mongodb.client.model.Filters.eq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.netbrain.xf.flowengine.amqp.AMQPClient;
import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFAgentInMemoryRepository;
import com.netbrain.xf.flowengine.daoinmemory.XFTaskInMemoryRepository;
import com.netbrain.xf.flowengine.scheduler.services.SchedulerServicesImpl;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentHeartBeatMessage;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentMetadata;
import com.netbrain.xf.flowengine.workerservermanagement.XFTaskSummary;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.xfcommon.XFCommon;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import io.netty.util.internal.StringUtil;

@Component
public class CommonUtil {
    private static Logger logger = LogManager.getLogger(CommonUtil.class.getSimpleName());

    public String localHostName = "localhost";
    public String localHostAddress = "127.0.0.1";

    @Value("${workerserver.selection.cpu.high.watermark}")
    private long cpuHighWatermark;

    @Value("${workerserver.selection.ram.high.watermark}")
    private long ramHighWatermark;

    @Value("${workerserver.crash.detection.missing.heartbeat.count}")
    private long missingHeartbeatCount;

    @Value("${taskengine.limit.taskcount}")
    public long maxTaskCountLimitInConfig;

    @Value("${flowengine.config.filepath}")
    private String configFilepath;

    @Autowired
    private XFTaskInMemoryRepository taskInMemoryRepository;

    @Autowired
    private XFTaskRepository taskRepository;

    @Autowired
    private XFTaskflowRepository taskflowRepository;

    @Autowired
    XFAgentInMemoryRepository xfAgentInMemoryRepository;

    @Autowired
    TaskController taskController;

    @Resource(name="ngsystem")
    private MongoTemplate ngsystemTemplate;

    private MongoDatabase mongoDatabase;

    @Autowired
    AMQPClient amqpClient;

    private Channel amqpChannel;

    @Value("${background.xfagentservermonitor.monitoringinterval_in_seconds}")
    private int monitoringinterval_in_seconds;

    public long getMissingHeartbeatCount() {
        return missingHeartbeatCount;
    }

    @PostConstruct
    public void initCommonUtil()
    {
        initLocalHostname();
        //initRabbitmqConnectionAndChannel();
    }
    private void initLocalHostname()
    {

        try {
            InetAddress ip = InetAddress.getLocalHost();
            localHostName = ip.getHostName();
            localHostAddress = ip.getHostAddress().trim();
        }
        catch (UnknownHostException uhe) {

            logger.warn("InetAddress.getLocalHost() failed with UnknownHostException: ", uhe);
        }
        catch (Exception e) {

            logger.warn("InetAddress.getLocalHost() failed with Exception: ", e);
        }
    }

    private void createRabbitChannel()
    {
        Connection mqConnection = amqpClient.getMqConnection();
        try {
            amqpChannel = mqConnection.createChannel();
        } catch (IOException e) {
            logger.error("Failed to create RabbitMQ channel", e);
            amqpChannel = null;
        }
    }

    private synchronized void closeRabbitmqChannel()
    {
        if (amqpChannel == null){
            return;
        }
        try {
            amqpChannel.close();
        } catch (Exception e) {
            logger.error("Failed to close RabbitMQ channel", e);
            amqpChannel = null;
        }
    }

    public static XFAgentHeartBeatMessage convertJsonStr2XFAgentHeartBeatMessage(String strJson )
    {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        XFAgentHeartBeatMessage retMsg = null;
        try {
            retMsg = mapper.readValue(strJson, XFAgentHeartBeatMessage.class);
        }
        catch (IOException ioe)
        {
            logger.warn("failed to convert to XFAgentHeartBeatMessage object from json string :" + strJson);
            retMsg = null;
        }

        return retMsg;
    }
    public static File getCertFile() {
        File certFile = null;
        File dir=new File("conf/certs/clientCerts");
        if(dir.isDirectory()) {
            File[] files = dir.listFiles();
            for(File file:files){
                if(file.isFile()) {
                   certFile=file;
                   break;
                }
            }
        }
        return certFile;
    }
    
    public static TrustManagerFactory getTrustManager() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        TrustManagerFactory trustManagerFactory=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ks = KeyStore.getInstance("Windows-ROOT");
        ks.load(null, null);
        trustManagerFactory.init(ks);
        return trustManagerFactory;
    }

    public boolean ProcessNewlyReceivedXFTaskSummaryInfo(HashMap<String, XFTaskSummary> selfTaskId2XFTaskSummaryDict) {
        boolean bRet = true;
        if (selfTaskId2XFTaskSummaryDict == null) {
            logger.debug("selfTaskId2XFTaskSummaryDict is null, received XFTaskSummary will be ignored this time.");
            return false;
        }

        bRet = this.taskInMemoryRepository.ProcessNewlyReceivedXFTaskSummaryInfo(selfTaskId2XFTaskSummaryDict);
        return bRet;
    }
    public boolean ProcessNewlyReceivedXFAgentInformation(XFAgent agentInfoInMemroy, XFAgent rcvdXFAgentInfo,  int infoFrom)
    {
        boolean bRet = true;
        if (agentInfoInMemroy == null )
        {
            logger.warn("agentInfoInMemroy is null with infoFrom ={}, received XFAgent will be ignored this time.", infoFrom );
            return false;
        }

        if (rcvdXFAgentInfo == null)
        {
            logger.warn("rcvdXFAgentInfo is null with infoFrom ={}, received XFAgent will be ignored this time.", infoFrom );
            return false;
        }

        Instant timestampInMem = agentInfoInMemroy.getXfAgentServerTimestampUTC();
        Instant timestampRcvd = rcvdXFAgentInfo.getXfAgentServerTimestampUTC();

        String serverNameInMemory = agentInfoInMemroy.getServerName();
        String xfAgentRunningInstanceIdInMemory = agentInfoInMemroy.getXfAgentRunningInstanceId();
        String xfAgentRunningInstanceId_rcvd = rcvdXFAgentInfo.getXfAgentRunningInstanceId();
        int processIdInMemory = agentInfoInMemroy.getXfAgentProcessId();
        XFAgentMetadata metadataInMem = this.xfAgentInMemoryRepository.GetOneXFAgentMetadata(serverNameInMemory);
        String uniqidInMem = metadataInMem.getUniqIdForEachUpdate();
        String uniqIdNewRcvd = rcvdXFAgentInfo.getUniqIdForEachUpdate();

        Instant instNow = Instant.now();
        Instant instInMem = metadataInMem.getFirsttimeReceivedThisUniqId();
        long xfagentNoUpdateForThatManySeconds = Duration.between(instInMem, instNow).toSeconds();

        if (timestampInMem != null && timestampRcvd != null && (timestampRcvd.isBefore(timestampInMem) || timestampRcvd.equals(timestampInMem))) {
            long xfagentUpdateInterval =  agentInfoInMemroy.getUpdateIntervalInSeconds();

            long missCount = Math.max(0L, xfagentNoUpdateForThatManySeconds / xfagentUpdateInterval);
            metadataInMem.setMissingHeartbeatCount(missCount);

            long threashold = missingHeartbeatCount * xfagentUpdateInterval;

            if (xfagentNoUpdateForThatManySeconds > threashold) {
                String strReason =
                        String.format("Flowengine detected that XFAgent process of serverName %s did not update for too long (%d > %d seconds), and will treat it as dead now.", serverNameInMemory, xfagentNoUpdateForThatManySeconds, threashold);
                logger.debug("---------- " + strReason);
                handleXFAgentCrash(serverNameInMemory, xfAgentRunningInstanceIdInMemory, processIdInMemory, metadataInMem, strReason, XFCommon.XFAgentBlacklistedReasonCode.REASON_DUE_TO_NO_UPDATE_TOO_LONG);
            }

            // if the newly received timestamp is still older than the information in Memory (because the inMemory info might have been updated by RabbitMQ heartbeat message), ignore it
            return true;
        } else {
            // rcvdXFAgentInfo is a new event
            // TODO: this block may be unreachable since if they have the same timestamp they should have the same uniqId
            if (uniqidInMem.equals(uniqIdNewRcvd)) {
                // the Agent hasn't reported a new event
                long xfagentUpdateInterval =  rcvdXFAgentInfo.getUpdateIntervalInSeconds();

                long missCount = Math.max(0L, xfagentNoUpdateForThatManySeconds / xfagentUpdateInterval);
                metadataInMem.setMissingHeartbeatCount(missCount);

                long threashold = missingHeartbeatCount * xfagentUpdateInterval;
                if (xfagentNoUpdateForThatManySeconds > threashold) {
                    String strReason =
                            String.format("Flowengine detected that XFAgent process of serverName %s did not update for too long (%d > %d seconds), and will treat it as dead now.", serverNameInMemory, xfagentNoUpdateForThatManySeconds, threashold);
                    logger.debug("---------- " + strReason);
                    handleXFAgentCrash(serverNameInMemory, xfAgentRunningInstanceIdInMemory, processIdInMemory, metadataInMem, strReason, XFCommon.XFAgentBlacklistedReasonCode.REASON_DUE_TO_NO_UPDATE_TOO_LONG);
                }
            }
        }

        try
        {
            int xfagentpid_rcvd = rcvdXFAgentInfo.getXfAgentProcessId();
            int xfagentpid_in_memory = agentInfoInMemroy.getXfAgentProcessId();

            // The criteria to identify a crash: XFAgent process ID has changed OR the RunningInstanceID has changed
            if ((xfagentpid_in_memory > 0) && (xfagentpid_rcvd > 0) && (xfagentpid_rcvd != xfagentpid_in_memory))
            {
                logger.warn("!!!!!!!!!!Flowengine detected that XFAgent process has been restarted on serverName {}, old pid {}, new pid {}", serverNameInMemory, xfagentpid_in_memory, xfagentpid_rcvd);
                HandleXFAgentProcessRestarted(serverNameInMemory, xfagentpid_in_memory);
            }

            if (StringUtil.isNullOrEmpty(xfAgentRunningInstanceIdInMemory) == false
                && StringUtil.isNullOrEmpty(xfAgentRunningInstanceId_rcvd) == false
                && !xfAgentRunningInstanceIdInMemory.equals(xfAgentRunningInstanceId_rcvd))
            {
                logger.warn("!!!!!!!!!!Flowengine detected that XFAgent process has been restarted on serverName {}, old RunningInstanceId {}, received RunningInstanceId {}",
                        serverNameInMemory, xfAgentRunningInstanceIdInMemory, xfAgentRunningInstanceId_rcvd);
                HandleXFAgentProcessRestarted(serverNameInMemory, xfagentpid_in_memory);
            }

            if (metadataInMem == null)
            {
                XFAgentMetadata newMeta = new XFAgentMetadata();
                newMeta.setServerName(serverNameInMemory);
                newMeta.setUniqIdForEachUpdate(rcvdXFAgentInfo.getUniqIdForEachUpdate());
                Instant firstimeForThisUniqId = Instant.now();
                newMeta.setFirsttimeReceivedThisUniqId(firstimeForThisUniqId);
                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(newMeta);
            }
            else {
                // Detect XFAgent is still doing update, update the uuid to the latest value, and update timestamp
                if (uniqidInMem.equals(uniqIdNewRcvd) == false)
                {
                    metadataInMem.setUniqIdForEachUpdate(uniqIdNewRcvd);
                    metadataInMem.setFirsttimeReceivedThisUniqId(Instant.now());
                    this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(metadataInMem);

                    if (rcvdXFAgentInfo.isCalculatedResourceUsage()) {
                        String strCannotReason = checkIfServerCanHandleNewTask(rcvdXFAgentInfo);
                        if (StringUtil.isNullOrEmpty(strCannotReason) == false) {
                            String newLine = System.getProperty("line.separator");//This will retrieve line separator dependent on OS.
                            String strJsonXFAgentInfo = XFAgent.convertXFagentobjectToJsonStr(rcvdXFAgentInfo);
                            String strReason =
                                    String.format("Worker server %s cannot handle new task because [ %s ], put it in blacklist, the json of XFAgent is:%s %s",
                                            serverNameInMemory,
                                            strCannotReason,
                                            newLine,
                                            strJsonXFAgentInfo);
                            logger.debug("----------" + strReason);
                            blacklistXFAgent(serverNameInMemory, metadataInMem, strReason, XFCommon.XFAgentBlacklistedReasonCode.REASON_DUE_TO_OVERLOAD);
                        } else {
                            // Principle to follow: once a XFAgent with a specific runningInstanceId has been put into blacklist once( which also means all the tasks associated with it has been marked as crashed)
                            // even when later we might receive heartbeat message (through DB or RabbitMQ) with the same runningInstanceId, we should NOT move this xfagent out of blacklist.

                            int blacklistedReasonCode = metadataInMem.getBlacklistedReasonCode();
                            boolean thisRunningInsanceIdHasAlreadyBeenMarkedAsDead = false;
                            if (!StringUtil.isNullOrEmpty(xfAgentRunningInstanceIdInMemory)
                                && xfAgentRunningInstanceIdInMemory.equals(xfAgentRunningInstanceId_rcvd)
                                && metadataInMem.isBlacklisted() == true
                                    && (blacklistedReasonCode == XFCommon.XFAgentBlacklistedReasonCode.REASON_DUE_TO_NO_UPDATE_TOO_LONG
                                    || blacklistedReasonCode == XFCommon.XFAgentBlacklistedReasonCode.REASON_DUE_TO_XFAGENT_RESTARTED))
                            {
                                thisRunningInsanceIdHasAlreadyBeenMarkedAsDead = true;
                            }

                            if (thisRunningInsanceIdHasAlreadyBeenMarkedAsDead )
                            {

                            }
                            else
                            {
                                if (infoFrom == XFCommon.XFAgentInformationFrom.FROM_DB_UPDATE || infoFrom == XFCommon.XFAgentInformationFrom.FROM_RABBITMQ_HB_MESSAGE) {
                                    metadataInMem.setMissingHeartbeatCount(0);
                                }
                                boolean wasBlackListed = metadataInMem.isBlacklisted();
                                metadataInMem.setBlacklisted(false);
                                metadataInMem.setBlacklistedReason("");
                                metadataInMem.setBlacklistedReasonCode(XFCommon.XFAgentBlacklistedReasonCode.REASON_NOT_BLACKLISTED);
                                this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(metadataInMem);
                                if (StringUtil.isNullOrEmpty(xfAgentRunningInstanceId_rcvd))
                                {
                                    xfAgentRunningInstanceId_rcvd = "";
                                }
                                if (wasBlackListed) {
                                    String strOutOfBlacklist = String.format("Flowengine successfully detected a XFAgent update from %s of runningInstanceId %s, it will be moved out of blacklist.", serverNameInMemory, xfAgentRunningInstanceId_rcvd);
                                    logger.info("++++++++++ " + strOutOfBlacklist);
                                }
                            }
                        }

                        int currTaskCount = metadataInMem.getRecentTaskCount();
                        if (currTaskCount != 0) {
                            String strInfo;
                            if (infoFrom == XFCommon.XFAgentInformationFrom.FROM_DB_UPDATE) {
                                strInfo =
                                        String.format("Flowengine successfully detected a XFAgent update from %s, the temporarily cached taskCount %d on this server will be reset to 0.", serverNameInMemory, currTaskCount);
                            }
                            else// if (infoFrom == XFCommon.XFAgentInformationFrom.FROM_RABBITMQ_HB_MESSAGE)
                            {
                                strInfo =
                                        String.format("Flowengine successfully received a XFAgent update msg from %s, the temporarily cached taskCount %d on this server will be reset to 0.", serverNameInMemory, currTaskCount);
                            }
                            logger.info("##### " + strInfo);
                        }
                        metadataInMem.setRecentTaskCount(0); // need to reset it, after flowengine gets a real xfagent data from XFAgent
                        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(metadataInMem);
                        if (timestampInMem == null
                                || (timestampRcvd != null && timestampRcvd.isAfter(timestampInMem))) {
                            // copy the newly Received XFAgent to memory
                            this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgent(rcvdXFAgentInfo);
                        }
                    } else {
                        // only basic heartbeat information is available
                        this.xfAgentInMemoryRepository.updateOneXFAgentHeartbeatInfoOnly(rcvdXFAgentInfo);
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.warn("ProcessNewlyReceivedXFAgentInformation exception with infoFrom="+ infoFrom, e);
            bRet = false;
        }

        return bRet;
    }

    private String checkIfServerCanHandleNewTask(XFAgent xfagentInfo) {

        String strReasonCannot = "";
        if (xfagentInfo.getServerCpuPercent() > cpuHighWatermark) {
            strReasonCannot = String.format("XFAgent server %s its current CPU %d > cpuHighWatermark %d", xfagentInfo.getServerName(), xfagentInfo.getServerCpuPercent(), cpuHighWatermark);
            return strReasonCannot;
        } else if (xfagentInfo.getServerPhysicalTotalMemoryInByte() != 0 &&
                xfagentInfo.getServerPhysicalAvailableMemoryInByte() != 0) {
            if (ramHighWatermark <= 10 || ramHighWatermark > 100) {
                ramHighWatermark = 90;
            }
            if (xfagentInfo.getServerPhysicalAvailableMemoryInByte() > (xfagentInfo.getServerPhysicalTotalMemoryInByte() * (100 - ramHighWatermark) / 100))
            {
                strReasonCannot = "";
            }
            else
            {
                strReasonCannot = String.format("XFAgent server %s its current getServerPhysicalAvailableMemoryInByte %d <= getServerPhysicalTotalMemoryInByte %d * (100 - ramHighWatermark %d) percent",
                        xfagentInfo.getServerName(), xfagentInfo.getServerPhysicalAvailableMemoryInByte(), xfagentInfo.getServerPhysicalTotalMemoryInByte(), ramHighWatermark);

            }
        }
        return strReasonCannot;
    }
    private void HandleXFAgentProcessRestarted(String xfagentServerName, int crashedXFAgentProcessId)
    {
        //this.taskInMemoryRepository.updateTaskWhenXFAgentProcessCrashed(xfagentServerName, crashedXFAgentProcessId);
        String strFinalReason = "CompletedCrash due to XFAgent Process " + crashedXFAgentProcessId + " on workerserver " + xfagentServerName + " terminated!";
        this.taskInMemoryRepository.processTaskByXFAgent(xfagentServerName, crashedXFAgentProcessId, (XFTask xfTask)->{
            if(xfTask != null && xfTask.getTaskStatus() <= XFCommon.TASKSTATUS_Running){
                taskController.processTaskEndMessage(xfTask.getSelfTaskId(), XFCommon.TASKSTATUS_CompletedCrash, strFinalReason);
                taskController.stopFollowedTask(xfTask.getSelfTaskId(), -1, strFinalReason);
            }

            return 0;
        });
    }

    private void blacklistXFAgent(String xfagentServerName, XFAgentMetadata metadataInMem, String strReason, int intReasonCode) {
        if (!metadataInMem.isBlacklisted()) {
            logger.warn("XFAgentServer ID {} is now added into Blacklist with this reason: {}.", xfagentServerName, strReason );
        } else {
            logger.debug("XFAgentServer ID {} is now added into Blacklist with this reason: {}.", xfagentServerName, strReason );
        }
        metadataInMem.setBlacklisted(true);
        metadataInMem.setBlacklistedReasonCode(intReasonCode);
        metadataInMem.setBlacklistedReason(strReason);
        this.xfAgentInMemoryRepository.AddOrUpdateOneXFAgentMetadata(metadataInMem);
    }

    public void handleXFAgentCrash(String xfagentServerName, String xfAgentRunningInstanceId, int xfAgentProcessId, XFAgentMetadata metadataInMem, String strReason,int intReasonCode )
    {
        XFAgent xfagent = xfAgentInMemoryRepository.GetOneXFAgent(xfagentServerName);
        if(xfagent != null && xfagent.isRetired()){
            xfAgentInMemoryRepository.DeleteOneXFAgent(xfagentServerName);
            xfAgentInMemoryRepository.DeleteOneXFAgentMetadata(xfagentServerName);
        }
        // step: mark this XFAgent server as shouldNotBeUsed in the InMemoryRepo
        this.blacklistXFAgent(xfagentServerName, metadataInMem, strReason, intReasonCode);

        // step: update taskStatus
        //this.taskInMemoryRepository.updateTaskWhenXFAgentProcessNoUpdateTooLong(xfagentServerName);

        String strFinalReason = "CompletedCrash due to failure to detect XFAgent Process on workerserver " + xfagentServerName + " updates itself for too long!";
        this.taskInMemoryRepository.processTaskByXFAgent(xfagentServerName, xfAgentProcessId, (XFTask xfTask)->{
            if(xfTask != null && xfTask.getTaskStatus() <= XFCommon.TASKSTATUS_Running){
                taskController.processTaskEndMessage(xfTask.getSelfTaskId(), XFCommon.TASKSTATUS_CompletedCrash, strFinalReason);
                taskController.stopFollowedTask(xfTask.getSelfTaskId(), -1, strFinalReason);
            }
            return 0;
        });


        String suicideMsgTTLInStr = Long.toString(this.monitoringinterval_in_seconds * 1000L);
        String strSuicideReason =
                String.format("TaskEngine on %s is telling XFAgent on %s to suicide, because: %s",
                        localHostName, xfagentServerName, strFinalReason);
        Map<String,Object> headers = new HashMap<>();
        headers.put(XFCommon.NBMSGVERSION, XFCommon.NBMSGVERSION_NB_IE_7_DOT_1);
        headers.put(XFCommon.STR_xfAgentRunningInstanceId, xfAgentRunningInstanceId);
        headers.put(XFCommon.RMAgent_suicide_reason, strSuicideReason);
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .type(XFCommon.command_RMAgent_please_suicide_now)
                .correlationId(xfagentServerName)
                .expiration(suicideMsgTTLInStr)
                .headers(headers)
                .contentType("application/json")
                .deliveryMode(2).build();

        createRabbitChannel();
        if (this.amqpChannel == null)
        {
            logger.warn("basicPublish  for {} will be given up because of failure to create a channel.", XFCommon.command_RMAgent_please_suicide_now );
            return;
        }
        try
        {
            publishMessage(props);
        }
        catch (Exception ex)
        {
            logger.warn("basicPublish encountered an exception for msgtype {}", XFCommon.command_RMAgent_please_suicide_now );
        }
        closeRabbitmqChannel();
    }

    // avoid concurrency when consuming and acking messages, see https://www.rabbitmq.com/api-guide.html
    private synchronized void publishMessage(AMQP.BasicProperties props) throws IOException {
        this.amqpChannel.basicPublish(XFCommon.RMAgent_exchange, XFCommon.RMAgent_command, props, null);
    }

    public boolean checkTaskExistence(String strTaskId)
    {
        if (StringUtil.isNullOrEmpty(strTaskId))
        {
            return false;
        }
        Optional<XFTask> xftaskOpt = this.taskInMemoryRepository.findById(strTaskId, false, false);

        if (xftaskOpt.isPresent())
        {
            return true;
        }

        int i = 0;
        while (i++ < 3)
        {
            try
            {
                xftaskOpt = this.taskRepository.findById(strTaskId);
                if (xftaskOpt.isPresent())
                {
                    return true;
                }
            }
            catch (Exception ex)
            {
                logger.warn(".taskRepository.findById({}) exception.", strTaskId, ex );
            }
        }
        return false;
    }

    public boolean checkIsValidExistingJobId(String idToCheck)
    {
        if (StringUtil.isNullOrEmpty(idToCheck))
        {
            return false;
        }

        int i = 0;
        while (i++ < 3)
        {
            try {
                mongoDatabase = ngsystemTemplate.getMongoDbFactory().getDb();
                FindIterable<Document> iteratble = mongoDatabase.getCollection(SchedulerServicesImpl.COLLECTION_NAME).find(eq("job.jobId", idToCheck));
                MongoCollection<Document> coll = mongoDatabase.getCollection(SchedulerServicesImpl.COLLECTION_NAME);
                long matchCount = coll.count(eq("job.jobId", idToCheck));
                if (matchCount > 0)
                {
                    return true;
                }
                else
                {
                    // in case the document in DB is deleted because of unscheduling a job, we need to check the taskflow for a matching jobid
                    if (this.taskflowRepository.countByJobId(idToCheck) > 0 )
                    {
                        return true;
                    }

                    return false;
                }

            }
            catch (Exception ex)
            {
                logger.warn("checkIsValidJobId({}) exception.", idToCheck, ex );
            }
        }

        return false;
    }

    public Properties loadConfigFile(){
        Properties flowengineProps = new Properties();
        try{
            File file = new File(configFilepath);
            FileInputStream in = new FileInputStream(file);
            flowengineProps.load(in);
            in.close();
        } catch (Exception e) {
            logger.error("Failed to load key configuration items from properties file " + configFilepath, e);
        }
        return flowengineProps;
    }

    public void writeConfigFile(Properties flowengineProps){
        try{
            File file = new File(configFilepath);
            FileOutputStream out = new FileOutputStream(file);
            flowengineProps.store(out, "");
            out.close();
        } catch (Exception e) {
            logger.error("Failed to write configuration items to properties file " + configFilepath, e);
        }
    }
}
