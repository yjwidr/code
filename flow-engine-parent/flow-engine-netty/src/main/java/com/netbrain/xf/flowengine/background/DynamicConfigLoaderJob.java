package com.netbrain.xf.flowengine.background;

import com.netbrain.xf.flowengine.dao.XFAgentRepository;
import com.netbrain.xf.flowengine.queue.ITaskQueueManager;
import com.netbrain.xf.flowengine.daoinmemory.XFAgentInMemoryRepository;
import com.netbrain.xf.model.XFAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.*;

@Component
@DisallowConcurrentExecution
public class DynamicConfigLoaderJob extends QuartzJobBean {

    private static Logger logger = LogManager.getLogger(DynamicConfigLoaderJob.class.getSimpleName());

    @Value("${flowengine.config.filepath}")
    private String configFilepath;

    @Autowired
    ITaskQueueManager taskQueueManager;

    @Autowired
    XFAgentInMemoryRepository xfAgentInMemoryRepository;

    protected List<String> loadWorkerservers() {
        try {
            File file = new File(configFilepath);
            if (file.exists()) {
                FileInputStream in = new FileInputStream(file);
                Properties flowengineProps = new Properties();
                flowengineProps.load(in);

                String workerServers = flowengineProps.getProperty("workerserver.servernames");
                in.close();

                if (workerServers != null) {
                    String[] serverNames = workerServers.split(",");
                    List<String> trimmedServerNames = new ArrayList<>(serverNames.length);
                    for (String serverName: serverNames) {
                        trimmedServerNames.add(StringUtils.trimAllWhitespace(serverName));
                    }
                    return trimmedServerNames;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load workerservers from properties file " + configFilepath, e);
        }
        return new ArrayList<String>(0);
    }

    protected void handleRetiredWorkerservers(List<String> inMemoryWorkerServers, List<String> configWorkerServers) {
        // find the worker servers in in-memory repository but not are in config
        // i.e. in-memory are [1,2], config are [2, 3], the result of in-memory is [1]
        inMemoryWorkerServers.removeAll(configWorkerServers);
        for (String retiredInMemoryWorkerServer: inMemoryWorkerServers) {
            logger.info("Retiring worker server {} due to config changes", retiredInMemoryWorkerServer);
            XFAgent xfagent = xfAgentInMemoryRepository.GetOneXFAgent(retiredInMemoryWorkerServer);
            if(xfagent != null){
                xfagent.setRetired(true);
            }
        }
    }

    protected void handleNewWorkerservers(List<String> inMemoryWorkerServers, List<String> configWorkerServers) {
        // find the worker servers are in config but not in in-memory repository
        // i.e. in-memory are [1,2], config are [2, 3], the result of config is [3]
        configWorkerServers.removeAll(inMemoryWorkerServers);
        // the rest of configWorkerServers are the newly added ones
        if (configWorkerServers.size() > 0) {
            logger.info("Adding {} worker servers due to config changes", configWorkerServers.size());
            taskQueueManager.addWorkerservers(configWorkerServers.toArray(new String[configWorkerServers.size()]));
        }
    }

    public void checkAndProcessConfigChange() {
        List<String> configWorkerServers = loadWorkerservers();
        List<XFAgent> inMemoryXFAgents = xfAgentInMemoryRepository.GetAllInMemoryXFAgent();
        List<String>  inMemoryXFAgentNames = new ArrayList<>();
        for (XFAgent knownXFAgent: inMemoryXFAgents) {
            String serverName = knownXFAgent.getServerName();
            if(knownXFAgent.isRetired() && configWorkerServers.contains(serverName)){
                knownXFAgent.setRetired(false);
            }

            if(!knownXFAgent.isRetired()){
                inMemoryXFAgentNames.add(serverName);
            }
        }

        handleRetiredWorkerservers(inMemoryXFAgentNames, configWorkerServers);

        // re-init knownXFAgentNames list since inMemoryXFAgentNames is modified in the above method
        inMemoryXFAgentNames = new ArrayList<>();
        for (XFAgent knownXFAgent: inMemoryXFAgents) {
            inMemoryXFAgentNames.add(knownXFAgent.getServerName());
        }

        handleNewWorkerservers(inMemoryXFAgentNames, configWorkerServers);
    }


    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        checkAndProcessConfigChange();
    }
}
