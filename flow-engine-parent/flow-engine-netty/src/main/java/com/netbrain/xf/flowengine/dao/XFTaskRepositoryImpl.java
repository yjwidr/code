package com.netbrain.xf.flowengine.dao;

import com.mongodb.client.result.UpdateResult;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.xfcommon.XFCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class XFTaskRepositoryImpl implements XFTaskRepositoryCustom
{
    private static Logger logger = LogManager.getLogger(XFTaskRepositoryImpl.class.getName());

    @Autowired
    MongoTemplate mongoTemplate;

    /**
     * @param origTask
     * @param newStatus
     * @return false if nothing got updated or the XFTask does not have a XFTaskflow
     */
    @Override
    public boolean updateStatus(XFTask origTask, int newStatus, String strTaskStatusFinalReason, Instant timestamp)
    {
        if (origTask.getXfTaskflow() == null) {
            return false;
        }

        origTask.setTaskStatus(newStatus);

        Criteria matchTaskId = Criteria.where("_id").is(origTask.getId());
        Criteria matchStatusNotFinal = new Criteria().where("taskStatus").lte(XFCommon.TASKSTATUS_Running);
        Criteria matchStatusMonotonic = new Criteria().where("taskStatus").lt(newStatus);
        Query query = new Query(new Criteria().andOperator(matchTaskId, matchStatusNotFinal, matchStatusMonotonic));

        Update update = new Update();
        update.set("taskStatus", newStatus);
        if (newStatus > XFCommon.TASKSTATUS_Running)
        {
            if (StringUtils.isEmpty(strTaskStatusFinalReason))
            {
                strTaskStatusFinalReason = "";
            }
            update.set("taskStatusFinalReason", strTaskStatusFinalReason);
            update.set("endTime", Instant.now());
        }

        if (timestamp != null) {
            if (newStatus == XFCommon.TASKSTATUS_Started) {
                update.set("dispatchTime", timestamp);
            }
        }
        UpdateResult result = mongoTemplate.updateFirst(query, update, origTask.getClass());
        if(result != null && result.getMatchedCount() > 0){
            if (origTask.getStatusIsFinal())
            {
                //TODO maybe only need to update statusPotentialDirty field
                origTask.getXfTaskflow().setStatusPotentialDirty(true);
                mongoTemplate.save(origTask.getXfTaskflow());
            }
            return true;
        } else {
            // nothing updated
            return false;
        }
    }

    /**
     * @param origTask
     * @param newServerName
     * @return false if nothing got updated or the XFTask does not have a XFTaskflow
     */
    @Override
    public boolean updateWorkerServerName(XFTask origTask, String newServerName)
    {
        if (origTask.getXfTaskflow() == null) {
            return false;
        }

        origTask.setWorkerMachineName(newServerName);
        Criteria matchTaskId = Criteria.where("_id").is(origTask.getId());
        Query query = new Query(new Criteria().andOperator(matchTaskId));
        Update update = new Update();
        update.set("workerMachineName", newServerName);

        return true;
    }

    @Override
    public boolean updateFinalTrigger(XFTask xfTask, boolean finalTrigger)
    {
        xfTask.setFinalTrigger(finalTrigger);

        Query query = new Query(Criteria.where("_id").is(xfTask.getId()));
        Update update = new Update();
        update.set("finalTrigger", finalTrigger);

        UpdateResult result = mongoTemplate.updateFirst(query, update, xfTask.getClass());
        if(result != null && result.getMatchedCount() > 0){
            return true;
        }
        return false;
    }

    @Override
    public boolean stopXFTasksByTaskflowId(String taskflowId, String strReason)
    {
        Criteria matchTaskflowId = new Criteria().where("taskflowId").is(taskflowId);
        Criteria matchStatus = new Criteria().where("taskStatus").lte(XFCommon.TASKSTATUS_Running);
        Query query = new Query( new Criteria().andOperator(matchTaskflowId, matchStatus));

        Update update = new Update();
        update.set("taskStatus", XFCommon.TASKSTATUS_Canceled);
        update.set(XFCommon.DBSTR_XFTASK_TASKSTATUSFINALREASON, strReason);

        UpdateResult result = mongoTemplate.updateMulti(query, update, XFTask.class);
        if(result != null ){
            return true;
        }
        return false;
    }

    @Override
    public boolean updateTaskWhenXFAgentProcessCrashed(String xfagentServerName, int crashedXFAgentProcessId)
    {
        Criteria matchStatusNotFinal = new Criteria().where(XFCommon.DBSTR_XFTASK_TASKSTATUS).lte(XFCommon.TASKSTATUS_Running);
        Criteria matchServerName = new Criteria().where(XFCommon.DBSTR_XFTASK_WORKERMACHINENAME).is(xfagentServerName);
        Criteria matchCrashedPid = new Criteria().where(XFCommon.DBSTR_XFAgentProcessId).is(crashedXFAgentProcessId);
        Query query = new Query( new Criteria().andOperator(matchStatusNotFinal, matchServerName,matchCrashedPid));

        Update update = new Update();
        update.set(XFCommon.DBSTR_XFTASK_TASKSTATUS, XFCommon.TASKSTATUS_CompletedCrash);
        String strReason = "CompletedCrash due to XFAgent Process " + crashedXFAgentProcessId + " on workerserver " + xfagentServerName + " terminated!";
        update.set(XFCommon.DBSTR_XFTASK_TASKSTATUSFINALREASON, strReason);

        UpdateResult result = mongoTemplate.updateMulti(query, update, XFTask.class);
        if(result != null){
            return true;
        }
        return false;
    }

    @Override
    public boolean updateTaskWhenXFAgentProcessNoUpdateTooLong(String xfagentServerName)
    {
        Criteria matchStatusNotFinal = new Criteria().where(XFCommon.DBSTR_XFTASK_TASKSTATUS).lte(XFCommon.TASKSTATUS_Running);
        Criteria matchServerName = new Criteria().where(XFCommon.DBSTR_XFTASK_WORKERMACHINENAME).is(xfagentServerName);
        Query query = new Query( new Criteria().andOperator(matchStatusNotFinal, matchServerName));

        Update update = new Update();
        update.set(XFCommon.DBSTR_XFTASK_TASKSTATUS, XFCommon.TASKSTATUS_CompletedCrash);
        String strReason = "CompletedCrash due to failure to detect XFAgent Process on workerserver " + xfagentServerName + " updates itself for too long!";
        update.set(XFCommon.DBSTR_XFTASK_TASKSTATUSFINALREASON, strReason);

        UpdateResult result = mongoTemplate.updateMulti(query, update, XFTask.class);
        if(result != null){
            return true;
        }
        return false;
    }

    @Override
    public List<XFTask> findAllUnfinishedTasks()
    {
        Criteria matchStatus = new Criteria().where("taskStatus").lt(XFCommon.TASKSTATUS_CompletedNormally);
        Query query = new Query( matchStatus);

        return mongoTemplate.find(query, XFTask.class);
    }

    @Override
    public List<XFTask> findTopNUnfinishedTasksByTaskPriorityAndSubmitTime(int nLimit)
    {
        Criteria matchStatus = new Criteria().where("taskStatus").lt(XFCommon.TASKSTATUS_CompletedNormally);
        Query query = new Query( matchStatus);

        if (nLimit <= 0) {
            return new ArrayList<XFTask>();
        } else {
            query.limit(nLimit);
            query.with(new Sort(Sort.Direction.DESC, "taskPriority").and(new Sort(Sort.Direction.ASC, "submitTime")));
            return mongoTemplate.find(query, XFTask.class);
        }
    }
}
