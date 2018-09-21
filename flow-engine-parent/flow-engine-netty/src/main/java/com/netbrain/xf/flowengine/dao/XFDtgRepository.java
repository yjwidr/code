package com.netbrain.xf.flowengine.dao;

import com.netbrain.xf.model.XFDtg;
import com.netbrain.xf.xfcommon.XFCommon;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface XFDtgRepository extends MongoRepository<XFDtg, String>, XFDtgRepositoryCustom {

    @Query("{'taskflowId':?0, '$and':[{'isFinalTriggerReceived':false}, {'dtgStatus':{$lte:" + XFCommon.DTGSTATUS_Running + "}}]}")
    List<XFDtg> findRunningDtgsForTaskflowId(String taskflowId);

    @Query("{'isFinalTriggerReceived':false, '$or':[ {'id':?0}, {'ancestorDtgIds':?0} ] }")
    List<XFDtg> findRunningDtgsForDtgId(String dtgId);

    @Query("{'taskflowId':?0, 'ancestorDtgIds':?1, '$and':[{'isFinalTriggerReceived':false}, {'dtgStatus':{$lte:" + XFCommon.DTGSTATUS_Running + "}}]}")
    List<XFDtg> findRunningDescendantDtgsForDtgId(String taskflowId, String dtgId);

    @Query("{'submitTime':{$lt: ?0}, 'triggerTime':null, 'isFinalTriggerReceived':false, 'dtgStatus':{$lte:" + XFCommon.DTGSTATUS_Running + "}}")
    List<XFDtg> findRunningDtgsSubmittedBefore(Instant submitEarlierThan, Pageable pageable);

    @Query("{'triggerTime':{$lt: ?0}, 'isFinalTriggerReceived':false, 'dtgStatus':{$lte:" + XFCommon.DTGSTATUS_Running + "}}")
    List<XFDtg> findRunningDtgsTriggeredBefore(Instant triggeredEarlierThan, Pageable pageable);

}