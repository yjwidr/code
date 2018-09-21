package com.netbrain.xf.flowengine.daoinmemory;

import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskRepositoryCustom;
import com.netbrain.xf.flowengine.metric.Metrics;
import com.netbrain.xf.flowengine.taskcontroller.SubmitTaskResult;
import com.netbrain.xf.flowengine.workerservermanagement.XFAgentMetadata;
import com.netbrain.xf.flowengine.workerservermanagement.XFTaskSummary;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import com.netbrain.xf.xfcommon.XFCommon;
import io.netty.util.internal.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class XFTaskInMemoryRepository  {
    private static Logger logger = LogManager.getLogger(XFTaskInMemoryRepository.class.getSimpleName());

    @Autowired
    private XFTaskRepository taskRepository;

    private Map<String, XFTask> xftaskHashMap = new ConcurrentHashMap<String, XFTask>();

    @Autowired
    private Metrics metrics;
    @PostConstruct
    void init()
    {
        metrics.setTaskInmemorySnapshotCallback((Long unuse)->{
            return (long)xftaskHashMap.size();
        });
    }

    public boolean deleteAllFromMemoryOnly()
    {
        this.xftaskHashMap.clear();
        return true;

    }
    public boolean deleteAllFromMemoryAndDB()
    {
        this.xftaskHashMap.clear();

        try
        {
            this.taskRepository.deleteAll();
        }
        catch (Exception e)
        {
            logger.warn("Exception in deleteAll" , e);
        }
        return true;
    }

    public boolean upsertXFTask_notUpdateDB(String selfTaskId, XFTask xftask) {
        if (StringUtil.isNullOrEmpty(selfTaskId)) {
            return false;
        }
        //Associates the specified value with the specified key in this map.
        // If the map previously contained a mapping for the key, the old value is replaced.
        xftaskHashMap.put(selfTaskId, xftask);
        return true;
    }

    public boolean upsertXFTask(String selfTaskId, XFTask xftask)
    {
        if (StringUtil.isNullOrEmpty(selfTaskId)) {
            return false;
        }
        //Associates the specified value with the specified key in this map.
        // If the map previously contained a mapping for the key, the old value is replaced.
        xftaskHashMap.put(selfTaskId, xftask);

        // now save it into DB if not in DB already.
        Optional<XFTask> xfTaskInDBOpt = Optional.empty();
        try {
            xfTaskInDBOpt = this.taskRepository.findById(selfTaskId);
        }
        catch (Exception e)
        {
            logger.warn("Exception in taskRepository.findById for " + selfTaskId, e );
        }
        if (xfTaskInDBOpt.isPresent() == false)
        {
            long pid = ProcessHandle.current().pid();
            String strTaskHistory = String.format("Task is saved in DB by floweninge of pid %d at %s", pid, Instant.now().toString());
            xftask.getTaskHistory().add(strTaskHistory);
            syncFromMemoryToDB(selfTaskId);
        }
        else
        {
            // if taskStatus is not final, we update taskStatus monotonically, if it is already final, we don't modify it any more.
            int taskStatusInDB = xfTaskInDBOpt.get().getTaskStatus();
            int taskStatusInMem = xftask.getTaskStatus();
            if (taskStatusInMem > taskStatusInDB )
            {
                if (!xfTaskInDBOpt.get().getStatusIsFinal())
                {
                    try {
                        this.taskRepository.updateStatus(xfTaskInDBOpt.get(), taskStatusInMem, "", null);
                    }
                    catch (Exception e)
                    {
                        logger.warn("Exception in taskRepository.upsertXFTask for taskid=" + selfTaskId, e);
                    }
                }
            }
            else if (taskStatusInMem < taskStatusInDB)
            {
                if (taskStatusInMem <= XFCommon.TASKSTATUS_Running)
                {
                    xftask.setTaskStatus(taskStatusInDB);
                }
            }
            else
            {
                // nothing to do if the status is the same
            }
        }
        return true;
    }

    public boolean deleteXFTask(String selfTaskId)
    {
        if (StringUtil.isNullOrEmpty(selfTaskId)) {
            return false;
        }
        xftaskHashMap.remove(selfTaskId);

        return true;
    }

    public boolean deleteManyXFTaskForTaskflowIDs(List<String> taskflowIDs)
    {
        if (taskflowIDs == null){
            return false;
        }

        // be carefule about "Iterators allow the caller to remove elements from the underlying collection during the iteration with well-defined semantics."
        Iterator<Map.Entry<String, XFTask>> iter = xftaskHashMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, XFTask> entry = iter.next();
            XFTask xfTask = entry.getValue();
            String taskflowId = xfTask.getTaskflowId();
            if (taskflowIDs.contains(taskflowId))
            {
                iter.remove();
            }
        }

        return true;
    }

    public boolean deleteAllFinishedTaskFromMemoryOnly()
    {

        // be carefule about "Iterators allow the caller to remove elements from the underlying collection during the iteration with well-defined semantics."
        Iterator<Map.Entry<String, XFTask>> iter = xftaskHashMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, XFTask> entry = iter.next();
            XFTask xfTask = entry.getValue();
            if (xfTask.getTaskStatus() > XFCommon.TASKSTATUS_Running)
            {
                iter.remove();
            }
        }

        return true;
    }

    public Optional<XFTask> findById(String selfTaskId)
    {
        return findById(selfTaskId, true, true);
    }

    public Optional<XFTask> findById(String selfTaskId, boolean bReadFromDB, boolean bSyncFromDB)
    {
        if (StringUtil.isNullOrEmpty(selfTaskId)) {
            return Optional.empty();
        }

        XFTask xfTaskInMem = xftaskHashMap.get(selfTaskId);
        if (!bReadFromDB) {
            if (xfTaskInMem == null)
            {
                return Optional.empty();
            }
            else
            {
                return Optional.of(xfTaskInMem);
            }
        }

        if (xfTaskInMem == null) {
            try {
                Optional<XFTask> xfTaskInDBOpt = taskRepository.findById(selfTaskId);
                if (xfTaskInDBOpt.isPresent())
                {
                    if (bSyncFromDB)
                    {
                        this.xftaskHashMap.put(xfTaskInDBOpt.get().getId(), xfTaskInDBOpt.get());
                    }

                    return xfTaskInDBOpt;
                }
            }
            catch (Exception e)
            {
                logger.warn("Exception in taskRepository.findById for " + selfTaskId, e );
            }

            return Optional.empty();
        }
        else {
            return Optional.of(xfTaskInMem);
        }
    }

    /**
     * Find XFTasks from in-memory repo for a given XFTaskflow.
     * @param taskflow
     * @return
     */
    public List<XFTask> findByTaskflow(XFTaskflow taskflow)
    {
        List<XFTask> foundTasks = new ArrayList<>();
        if (null == taskflow) {
            return foundTasks;
        }

        for (XFTask task: xftaskHashMap.values()) {
            if (task.getXfTaskflow() == taskflow) {
                foundTasks.add(task);
            }
        }

        return foundTasks;
    }

    public XFTask getXFTask(String selftaskid)
    {
        if (StringUtil.isNullOrEmpty(selftaskid)) {
            return null;
        }

        XFTask xfTaskInMem = xftaskHashMap.get(selftaskid);
        return xfTaskInMem;
    }

    public boolean syncFromMemoryToDB(String selftaskid)
    {
        if (StringUtil.isNullOrEmpty(selftaskid)) {
            return false;
        }

        XFTask xfTaskInMem = xftaskHashMap.get(selftaskid);
        if (xfTaskInMem == null)
        {
            return false;
        }

        try {
            this.taskRepository.save(xfTaskInMem);
        }
        catch (Exception e)
        {
            logger.warn("syncFromMemoryToDB failed for selftaskid " + selftaskid, e);
            return false;
        }

        return true;
    }

    public boolean syncFromDBToMemory(String selfTaskId)
    {
        boolean bRet = true;
        if (StringUtil.isNullOrEmpty(selfTaskId)) {
            return false;
        }

        XFTask xfTaskInDB = null;
        try {
            Optional<XFTask> xfTaskInDBOpt = this.taskRepository.findById(selfTaskId);
            if (xfTaskInDBOpt.isPresent())
            {
                xfTaskInDB = xfTaskInDBOpt.get();
                bRet = this.upsertXFTask_notUpdateDB(selfTaskId, xfTaskInDB);
            }
            else
            {
                logger.warn("syncFromDBToMemory failed because failure to find xftask in DB for selftaskid " + selfTaskId);
                bRet = false;
            }
        }
        catch (Exception e)
        {
            logger.warn("syncFromDBToMemory failed for selftaskid " + selfTaskId, e);
            bRet = false;
        }

        return bRet;
    }

    public Set<Map.Entry<String, XFTask>> getAllXFTasks()
    {
        return xftaskHashMap.entrySet();
    }

    public int getXFTaskCount()
    {
        return xftaskHashMap.size();
    }

    public void performSyncWithDB()
    {
        try
        {
            for (Map.Entry<String, XFTask> entry: xftaskHashMap.entrySet()) {
                XFTask aXFTask = entry.getValue();
                if (aXFTask == null ) {
                    continue;
                }

                Optional<XFTask> taskInDBOpt = this.taskRepository.findById(aXFTask.getId());
                if (taskInDBOpt.isPresent() == false)
                {
                    try {
                        this.taskRepository.save(aXFTask);
                    }
                    catch (Exception saveEx)
                    {
                        logger.warn("performSyncWithDB taskRepository.save failed exception for taskflowId=" + aXFTask.getId() , saveEx);
                    }
                }
                else
                {
                    XFTask taskInDB = taskInDBOpt.get();
                    if (!taskInDB.getStatusIsFinal() && taskInDB.getTaskStatus() < aXFTask.getTaskStatus())
                    {
                        //xftodo, we should only update the status, but we don't have the api now, enhance it later if necessary
                        this.taskRepository.updateStatus(taskInDB, aXFTask.getTaskStatus(), "", null);
                    }
                    else if (!aXFTask.getStatusIsFinal() && aXFTask.getTaskStatus() < taskInDB.getTaskStatus())
                    {
                        this.xftaskHashMap.put(aXFTask.getId(), taskInDB);
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.warn("performSyncWithDB failed with exception:" , e);
        }
    }


    public boolean ProcessNewlyReceivedXFTaskSummaryInfo(HashMap<String, XFTaskSummary> selfTaskId2XFTaskSummaryDict)
    {
        boolean bRet = true;
        for(Map.Entry<String, XFTaskSummary> entry : selfTaskId2XFTaskSummaryDict.entrySet()) {
            String selfTaskId = entry.getKey();
            XFTaskSummary taskSummary = entry.getValue();

            XFTask xfTaskInMem = this.xftaskHashMap.get(selfTaskId);
            if (xfTaskInMem == null)
            {
                continue;
            }
            int statusInMem = xfTaskInMem.getTaskStatus();
            int statusRcvd = taskSummary.taskStatusAsInt;
            if (statusInMem < XFCommon.TASKSTATUS_CompletedNormally && statusInMem < statusRcvd)
            {
                this.updateStatus(xfTaskInMem, statusRcvd, taskSummary.taskStatusFinalReason, null);
            }
        }

        return bRet;
    }
    /////////////////////////////////////////////////////////////////////////////////
    // below are functions that have the same function name as the XFTaskRepository//
    /////////////////////////////////////////////////////////////////////////////////

    //@Query("{'xfTaskflow.id':?0, 'parentTaskId': \"\" ,'taskStatus': {'$lte' : " + XFCommon.TASKSTATUS_Running + "}}")
    // For finished tasks, they may be removed from in-memory repository so they don't count
    public List<XFTask> findRunningRootTasksForTaskflowId(String taskflowId)
    {
        List<XFTask> retList = new ArrayList<XFTask>();
        for (Map.Entry<String, XFTask> entry: xftaskHashMap.entrySet()) {
            XFTask aXFTask = entry.getValue();
            if (aXFTask == null ) {
                continue;
            }

            if (aXFTask.getTaskflowId().equals(taskflowId) && aXFTask.isRootTask() && aXFTask.getTaskStatus() <= XFCommon.TASKSTATUS_Running)
            {
                retList.add(aXFTask);
            }
        }
        return retList;
    }

    //@Query("{ 'taskStatus' : {'$lte' : " + XFCommon.TASKSTATUS_Running + "}, 'associatedDtgIds' : ?0}")
    public List<XFTask> findRunningTasksForDtgId(String dtgId)
    {
        List<XFTask> retList = new ArrayList<XFTask>();
        for (Map.Entry<String, XFTask> entry: xftaskHashMap.entrySet()) {
            XFTask aXFTask = entry.getValue();
            if (aXFTask == null ) {
                continue;
            }

            List<String> dtgs = aXFTask.getAssociatedDtgIds();
            if ( aXFTask.getTaskStatus() <= XFCommon.TASKSTATUS_Running && dtgs.contains(dtgId))
            {
                retList.add(aXFTask);
            }
        }
        return retList;
    }

    public List<XFTask> findOlderUnfinishedTasksForDtgId(String taskflowId, String dtgId, XFTask selfTask)
    {
        List<XFTask> retList = new ArrayList<XFTask>();
        for (Map.Entry<String, XFTask> entry: xftaskHashMap.entrySet()) {
            XFTask aXFTask = entry.getValue();
            if (aXFTask == null ) {
                continue;
            }
            if (aXFTask.getId().equals(selfTask.getId()))
            {
                // do not include myself
                continue;
            }
            List<String> dtgs = aXFTask.getAssociatedDtgIds();
            if (aXFTask.getTaskflowId().equals(taskflowId)
                    && aXFTask.getSubmitTime() != null && selfTask.getSubmitTime() != null
                    && (aXFTask.getSubmitTime().isBefore(selfTask.getSubmitTime()) || aXFTask.getSubmitTime().equals(selfTask.getSubmitTime()))
                    && aXFTask.getTaskStatus() <= XFCommon.TASKSTATUS_Running
                    && dtgs.contains(dtgId))
            {
                // 1. older tasks, 2. hasn't finished, 3. using the dtgId
                retList.add(aXFTask);
            }
        }
        return retList;
    }

    /**
     * just update info in memory
     * @param xftask
     * @param xfagentProcessId
     * @param xfagentServerName
     */
    public void updateXFAgentInfo(XFTask xftask, int xfagentProcessId, String xfagentServerName){
        if(xftask.getXFAgentProcessId() < 0 && xfagentProcessId > 0){
            xftask.setXFAgentProcessId(xfagentProcessId);
        }
        String workerMachineName = xftask.getWorkerMachineName();
        if((workerMachineName == null || workerMachineName.isEmpty()) && xfagentServerName != null && !xfagentServerName.isEmpty()){
            xftask.setWorkerMachineName(xfagentServerName);
        }
    }


    /**
     *
     * @param xfTask
     * @param newStatus
     * @param strTaskStatusFinalReason
     * @param timestamp when this is null, do not update any timestamp field. If this is not null and newStatus
     *                  is TASKSTATUS_Started, dispatchTime is updated
     * @return
     */
    public boolean updateStatus(XFTask xfTask, int newStatus, String strTaskStatusFinalReason, Instant timestamp) {
        if(newStatus > xfTask.getTaskStatus()){
            xfTask.setTaskStatus(newStatus);
            xfTask.setTaskStatusFinalReason(strTaskStatusFinalReason);
        }

        // we don't need to explicitly update the in memory repository

        // mark dispatch time
        if (timestamp != null && newStatus == XFCommon.TASKSTATUS_Started) {
            xfTask.setDispatchTime(timestamp);
        }

        try {

            Optional<XFTask> xfTaskInDBOpt = taskRepository.findById(xfTask.getId());
            if (xfTaskInDBOpt.isPresent() == false)
            {
                taskRepository.save(xfTask);
            }
            // if taskStatus is not final, we update taskStatus monotonically, if it is already final, we don't modify it any more.
            else if (!xfTaskInDBOpt.get().getStatusIsFinal() && xfTask.getTaskStatus() > xfTaskInDBOpt.get().getTaskStatus()) {
                taskRepository.updateStatus(xfTask, newStatus, strTaskStatusFinalReason, timestamp);
            }
            else {
                // if taskStatus is not final, we update taskStatus monotonically, if it is already final, we don't modify it any more.
                int taskStatusInDB = xfTaskInDBOpt.get().getTaskStatus();
                int taskStatusInMem = xfTask.getTaskStatus();
                if (taskStatusInMem > taskStatusInDB) {
                    if (!xfTaskInDBOpt.get().getStatusIsFinal()) {
                        try {
                            this.taskRepository.updateStatus(xfTaskInDBOpt.get(), taskStatusInMem, "", timestamp);
                        } catch (Exception e) {
                            logger.warn("Exception in taskRepository.updateStatus for taskid=" + xfTask.getId(), e);
                        }
                    }
                } else if (taskStatusInMem < taskStatusInDB) {
                    if (taskStatusInMem <= XFCommon.TASKSTATUS_Running) {
                        xfTask.setTaskStatus(taskStatusInDB);
                    }
                } else {
                    // nothing to do if the status is the same

                }
            }
        }
        catch (Exception e)
        {
            logger.warn("updateStatus for task {} with new taskStatus {} exception: {}", xfTask.getId(), newStatus, e.toString());
        }

        return true;
    }

    public boolean updateWorkerServerName(XFTask origTask, String newServerName)
    {
        if (origTask.getXfTaskflow() == null) {
            return false;
        }

        taskRepository.updateWorkerServerName(origTask, newServerName);

        return true;
    }

    public boolean updateFinalTrigger(XFTask xfTask, boolean finalTrigger) {

        xfTask.setFinalTrigger(finalTrigger);

        try {
            taskRepository.updateFinalTrigger(xfTask, finalTrigger);
        }
        catch (Exception e)
        {
            logger.warn("updateFinalTrigger exception: ", e);
        }

        return true;
    }


    public boolean stopXFTasksByTaskflowId(String taskflowId, String strReason) {

        for (Map.Entry<String, XFTask> entry: xftaskHashMap.entrySet()) {
            XFTask aXFTask = entry.getValue();
            if (aXFTask == null ) {
                continue;
            }

            List<String> dtgs = aXFTask.getAssociatedDtgIds();
            if (aXFTask.getTaskflowId().equals(taskflowId) && aXFTask.getTaskStatus() <= XFCommon.TASKSTATUS_Running )
            {
                aXFTask.setTaskStatus(XFCommon.TASKSTATUS_Canceled);
                aXFTask.setTaskStatusFinalReason(strReason);
            }
        }

        try {
            taskRepository.stopXFTasksByTaskflowId(taskflowId, strReason);
        }
        catch (Exception e)
        {
            logger.warn("stopXFTasksByTaskflowId exception: ", e);
        }

        return true;
    }


    public boolean updateTaskWhenXFAgentProcessCrashed(String xfagentServerName, int crashedXFAgentProcessId) {

        for (Map.Entry<String, XFTask> entry: xftaskHashMap.entrySet()) {
            XFTask aXFTask = entry.getValue();
            if (aXFTask == null ) {
                continue;
            }

            if ( aXFTask.getTaskStatus() <= XFCommon.TASKSTATUS_Running && xfagentServerName.equals(aXFTask.getWorkerMachineName()) && aXFTask.getXFAgentProcessId() == crashedXFAgentProcessId)
            {
                aXFTask.setTaskStatus(XFCommon.TASKSTATUS_CompletedCrash);
                String strReason = "CompletedCrash due to XFAgent Process " + crashedXFAgentProcessId + " on workerserver " + xfagentServerName + " terminated!";
                aXFTask.setTaskStatusFinalReason(strReason);
            }
        }

        try {
            taskRepository.updateTaskWhenXFAgentProcessCrashed(xfagentServerName, crashedXFAgentProcessId);
        }
        catch (Exception e)
        {
            logger.warn("updateTaskWhenXFAgentProcessCrashed exception: ", e);
        }

        return true;
    }


    public boolean updateTaskWhenXFAgentProcessNoUpdateTooLong(String xfagentServerName ) {
        for (Map.Entry<String, XFTask> entry: xftaskHashMap.entrySet()) {
            XFTask aXFTask = entry.getValue();
            if (aXFTask == null ) {
                continue;
            }

            List<String> dtgs = aXFTask.getAssociatedDtgIds();
            if ( aXFTask.getTaskStatus() <= XFCommon.TASKSTATUS_Running && aXFTask.getWorkerMachineName().equals(xfagentServerName) )
            {
                aXFTask.setTaskStatus(XFCommon.TASKSTATUS_CompletedCrash);
                String strReason = "CompletedCrash due to failure to detect XFAgent Process on workerserver " + xfagentServerName + " updates itself for too long!";
                aXFTask.setTaskStatusFinalReason(strReason);
            }
        }

        try {
            taskRepository.updateTaskWhenXFAgentProcessNoUpdateTooLong(xfagentServerName);
        }
        catch (Exception e)
        {
            logger.warn("updateTaskWhenXFAgentProcessNoUpdateTooLong exception: ", e);
        }

        return true;
    }

    public boolean processTaskByXFAgent(String xfagentServerName, int xfagentProcessId, Function<XFTask, Integer> process){
        try{
            for (Map.Entry<String, XFTask> entry: xftaskHashMap.entrySet()) {
                XFTask aXFTask = entry.getValue();
                if (aXFTask == null ) {
                    continue;
                }

                if(xfagentServerName.equals(aXFTask.getWorkerMachineName()) && aXFTask.getXFAgentProcessId() == xfagentProcessId){
                    process.apply(aXFTask);
                }
            }
        } catch (Exception e){
            logger.warn("processTaskByXFAgent exception: ", e);
        }

        return true;
    }
}
