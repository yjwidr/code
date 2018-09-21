package com.netbrain.xf.flowengine.dao;

import com.mongodb.client.result.UpdateResult;
import com.netbrain.xf.model.XFDtg;
import com.netbrain.xf.xfcommon.XFCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.List;

public class XFDtgRepositoryImpl implements XFDtgRepositoryCustom{
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean incTriggerReceivedTotalTimes(XFDtg xfDtg, int times) {
        Query query = new Query(Criteria.where("_id").is(xfDtg.getId()));
        Update update = new Update();
        update.inc("triggerReceivedTotalTimes", times);
        update.set("triggerTime", Instant.now());
        XFDtg newXFDtg = mongoTemplate.findAndModify(query, update, xfDtg.getClass());
        if(newXFDtg != null){
            xfDtg.setTriggerReceivedTotalTimes(newXFDtg.getTriggerReceivedTotalTimes());
            return true;
        }
        return false;
    }

    @Override
    public boolean updateFinalTriggerReceived(XFDtg xfDtg, boolean finalTrigger) {
        Query query = new Query(Criteria.where("_id").is(xfDtg.getId()));
        Update update = new Update();
        update.set("isFinalTriggerReceived", finalTrigger);
        if (finalTrigger) {
            update.set("dtgStatus", XFCommon.DTGSTATUS_CompletedNormally);
        }
        UpdateResult result = mongoTemplate.updateFirst(query, update, xfDtg.getClass());
        if(result != null && result.getMatchedCount() > 0){
            return true;
        }
        return false;
    }

    @Override
    public boolean updateDtgStatusByJobIdOrTaskflowId(String strJobIdOrTaskflowId, int newDtgStatus)
    {
        Criteria matchJobId = new Criteria().where("jobId").is(strJobIdOrTaskflowId);
        Criteria matchTaskflowId = new Criteria().where("taskflowId").is(strJobIdOrTaskflowId);
        Query query = new Query( new Criteria().orOperator(matchJobId, matchTaskflowId));

        Update update = new Update();
        update.set("dtgStatus", newDtgStatus);

        XFDtg xfDtg = new XFDtg();
        UpdateResult result = mongoTemplate.updateMulti(query, update, xfDtg.getClass());
        if(result != null && result.getMatchedCount() > 0){
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteByJobIdOrTaskflowId(String strJobIdOrTaskflowId)
    {
        Criteria matchJobId = new Criteria().where("jobId").is(strJobIdOrTaskflowId);
        Criteria matchTaskflowId = new Criteria().where("taskflowId").is(strJobIdOrTaskflowId);
        Query query = new Query( new Criteria().orOperator(matchJobId, matchTaskflowId));
        XFDtg nouseDtg = new XFDtg();
        mongoTemplate.findAllAndRemove(query, nouseDtg.getClass(), "XFDtg");

        return true;
    }

    private Query constructDtgByJobIdOrTaskIdQuery(String strJobIdOrTaskflowId) {
        Criteria matchJobId = new Criteria().where("jobId").is(strJobIdOrTaskflowId);
        Criteria matchTaskflowId = new Criteria().where("taskflowId").is(strJobIdOrTaskflowId);
        Criteria matchStatus = new Criteria().where("dtgStatus").lte(XFCommon.DTGSTATUS_Running);
        return new Query( new Criteria().orOperator(matchJobId, matchTaskflowId).andOperator(matchStatus));
    }

    @Override
    public boolean stopDtgsByJobIdOrTaskflowId(String strJobIdOrTaskflowId)
    {
        Update update = new Update();
        update.set("dtgStatus", XFCommon.DTGSTATUS_Canceled);

        UpdateResult result = mongoTemplate.updateMulti(constructDtgByJobIdOrTaskIdQuery(strJobIdOrTaskflowId),
                update, XFDtg.class);
        if(result != null){
            return true;
        }
        return false;
    }

    @Override
    public List<XFDtg> findDtgsByJobIdOrTaskflowId(String strJobIdOrTaskflowId) {
        return mongoTemplate.find(constructDtgByJobIdOrTaskIdQuery(strJobIdOrTaskflowId), XFDtg.class);
    }



    @Override
    public List<XFDtg> findDtgsByJobId(String strJobId) {
        Criteria matchJobId = new Criteria().where("jobId").is(strJobId);
        Criteria matchStatus = new Criteria().where("dtgStatus").lte(XFCommon.DTGSTATUS_Running);
        Query theQuery = new Query( new Criteria().andOperator(matchJobId, matchStatus));
        return mongoTemplate.find(theQuery, XFDtg.class);
    }

    @Override
    public List<XFDtg> findDtgsByTaskflowId(String strTaskflowId) {
        Criteria matchTaskflowId = new Criteria().where("taskflowId").is(strTaskflowId);
        Criteria matchStatus = new Criteria().where("dtgStatus").lte(XFCommon.DTGSTATUS_Running);
        Query theQuery = new Query( new Criteria().andOperator(matchTaskflowId, matchStatus));
        return mongoTemplate.find(theQuery, XFDtg.class);
    }

    @Override
    public void dropCollection()
    {
        mongoTemplate.dropCollection("XFDtg");
    }
}
