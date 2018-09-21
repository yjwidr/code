package com.netbrain.xf.flowengine.dao;

import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import com.netbrain.xf.xfcommon.XFCommon;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface XFTaskflowRepository extends MongoRepository<XFTaskflow, String>, XFTaskflowRepositoryCustom {
    boolean existsByJobIdAndStatusIn(String jobId, Collection states);

    boolean existsByJobId(String jobId);

    @Query("{'id':?0}")
    XFTaskflow findTaskflowById(String id);
}
