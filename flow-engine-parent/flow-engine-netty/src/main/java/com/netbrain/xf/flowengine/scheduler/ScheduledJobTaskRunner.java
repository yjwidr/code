package com.netbrain.xf.flowengine.scheduler;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.netbrain.xf.flowengine.scheduler.services.SchedulerServicesImpl;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.model.XFTask;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.quartz.JobDataMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

@Component
public class ScheduledJobTaskRunner {
    private static Logger logger = LogManager.getLogger(ScheduledJobTaskRunner.class.getName());
    @Autowired
    TaskController taskController;

    @Value("${scheduler.onlyacceptedusername}")
    private String onlyAcceptedUserName;

    @Resource(name="ngsystem")
    private MongoTemplate ngsystemTemplate;

    private MongoDatabase mongoDatabase;

    public void startNextRun(JobDataMap jobDataMap, String jobId) {
        mongoDatabase = ngsystemTemplate.getMongoDbFactory().getDb();
        FindIterable<Document> iteratble = mongoDatabase.getCollection(SchedulerServicesImpl.COLLECTION_NAME).find(eq("job.jobId", jobId));
        Document jobDocument = null;
        for (Document document : iteratble) {
            jobDocument = document.get("job", Document.class);
        }

        if (jobDocument == null) {
            logger.warn("Failed to find job definition for job id: " + jobId);
            return;
        }

        if (StringUtil.isNullOrEmpty(onlyAcceptedUserName) == false) {
            if (jobDataMap.containsKey("userName")) {
                String jobUserName = jobDataMap.getString("userName");
                if (onlyAcceptedUserName.equals(jobUserName) == false)
                {
                    logger.info("flowengine scheduler configured scheduler.onlyacceptedusername {} does not match the scheduled job's user name {}, the job will not be run by this flowengine!", onlyAcceptedUserName, jobUserName);
                    return;
                }
            }
        }
        XFTask task = new XFTask();
        if ((jobDocument.containsKey("active") && jobDocument.getBoolean("active")) || !jobDocument.containsKey("active")) {
            task.setId(UUID.randomUUID().toString());
            task.setRootTaskId(task.getId());
            task.setJobId(jobId);
            task.setTaskType(jobDataMap.getString("jobType"));
            task.setShortDescription(jobDataMap.getString("ShortDescription"));
            task.setWorkerRestartTimes(jobDataMap.getInt("WorkerRestartTimes"));
            task.setNeedBroadCallbackToAllApiServer(jobDataMap.getBoolean("needBroadCallbackToAllApiServer"));
            task.setJobRunCategory(XFTask.CATEGORY_Scheduled);
            task.setUserName(jobDataMap.getString("userName"));
            task.setUserIP(jobDataMap.getString("userIPAddress"));
            task.setDomainId(jobDataMap.getString("domainId"));
            task.setDomainDbName(jobDataMap.getString("domainDbName"));
            task.setTenantId(jobDataMap.getString("tenantId"));
            task.setTenantDbName(jobDataMap.getString("tenantDbName"));
            task.setTaskCallbackQueue(jobDataMap.getString("callbackqueue"));
            task.setTaskPriority(jobDataMap.getInt("priority"));
            task.setTaskParameters(jobDataMap.getString("parameters"));
            taskController.submitTask(task, null);
            logger.info("send parameters to queue for jobId={}, taskId={}" ,jobId, task.getId());
        } else {
            logger.debug("skip running job for jobId={} since the job is not active" ,jobId);
        }
    }
}
