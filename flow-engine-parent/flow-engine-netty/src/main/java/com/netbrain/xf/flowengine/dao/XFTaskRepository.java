package com.netbrain.xf.flowengine.dao;

import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.xfcommon.XFCommon;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface XFTaskRepository extends MongoRepository<XFTask, String>, XFTaskRepositoryCustom
{
    @Query("{'xfTaskflow.id':?0, 'parentTaskId': \"\" ,'taskStatus': {'$lte' : " + XFCommon.TASKSTATUS_Running + "}}")
    List<XFTask> findRunningRootTasksForTaskflowId(String taskflowId);

    @Query("{ 'taskStatus' : {'$lte' : " + XFCommon.TASKSTATUS_Running + "}, 'associatedDtgIds' : ?0}")
    List<XFTask> findRunningTasksForDtgId(String dtgId);

    @Query(value = "{ 'workerMachineName': ?0, 'taskType': {'$in': ?1}, 'taskStatus' : {'$lte' : " + XFCommon.TASKSTATUS_Running + "}}", count = true)
    long countAssignedTasksByWorker(String workerServerName, List<String> limitedTaskTypes);

    @Query("{ 'taskflowId':?0, 'taskStatus' : {'$in' : [" + XFCommon.TASKSTATUS_Started + ", " + XFCommon.TASKSTATUS_Running + "]}, 'associatedDtgIds' : ?1}")
    List<XFTask> findStartedOrRunningTasksForDtgId(String taskflowId, String dtgId);
}
