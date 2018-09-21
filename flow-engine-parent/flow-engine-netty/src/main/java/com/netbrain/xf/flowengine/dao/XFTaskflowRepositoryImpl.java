package com.netbrain.xf.flowengine.dao;

import com.netbrain.xf.model.XFTaskflow;
import com.netbrain.xf.xfcommon.XFCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class XFTaskflowRepositoryImpl implements XFTaskflowRepositoryCustom {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<XFTaskflow> findRunningTaskflowByJobIdOrTaskflowId(String jobIdOrTaskflowId) {
        Criteria matchJobId = new Criteria().where("jobId").is(jobIdOrTaskflowId);
        Criteria matchTaskflowId = new Criteria().where("id").is(jobIdOrTaskflowId);
        Criteria matchStatus = new Criteria().where("status").lt(XFCommon.TASKFLOWSTATUS_CompletedNormally);
        Query query = new Query( new Criteria().orOperator(matchJobId, matchTaskflowId).andOperator(matchStatus));

        return mongoTemplate.find(query, XFTaskflow.class);
    }

    @Override
    public List<XFTaskflow> findAllUnfinishedTaskflow()
    {
        Criteria matchStatus = new Criteria().where("status").lt(XFCommon.TASKFLOWSTATUS_CompletedNormally);
        Query query = new Query( matchStatus);

        return mongoTemplate.find(query, XFTaskflow.class);
    }

    @Override
    public long countByJobId(String jobId)
    {
        Criteria matchJobId = new Criteria().where("jobId").is(jobId);
        Query query = new Query(matchJobId);

        return mongoTemplate.count(query,  XFTaskflow.class);

    }
}
