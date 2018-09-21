package com.netbrain.xf.flowengine.daoinmemory;

import com.netbrain.xf.flowengine.dao.XFTaskRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepository;
import com.netbrain.xf.flowengine.dao.XFTaskflowRepositoryCustom;
import com.netbrain.xf.flowengine.taskcontroller.TaskController;
import com.netbrain.xf.model.XFAgent;
import com.netbrain.xf.model.XFTask;
import com.netbrain.xf.model.XFTaskflow;
import com.netbrain.xf.xfcommon.XFCommon;
import io.netty.util.internal.StringUtil;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class XFTaskflowInMemoryRepository
{
    private static Logger logger = LogManager.getLogger(XFTaskflowInMemoryRepository.class.getSimpleName());

    @Autowired
    private XFTaskflowRepository taskflowRepository;

    private Map<String, XFTaskflow> xftaskflowHashMap = new ConcurrentHashMap<String, XFTaskflow>();

    public Map<String, XFTaskflow> getXftaskflowHashMap() {
        return xftaskflowHashMap;
    }

    public boolean deleteAll()
    {
        this.xftaskflowHashMap.clear();
        return true;
    }

    public boolean upsertXFTaskflow(String taskflowid, XFTaskflow xftaskflow)
    {
        if (StringUtil.isNullOrEmpty(taskflowid)) {
            return false;
        }
        //Associates the specified value with the specified key in this map.
        // If the map previously contained a mapping for the key, the old value is replaced.
        xftaskflowHashMap.put(taskflowid, xftaskflow);

        syncFromMemoryToDB(taskflowid);
        return true;
    }

    public boolean upsertXFTaskflow_notUpdateDB(String taskflowid, XFTaskflow xftaskflow)
    {
        if (StringUtil.isNullOrEmpty(taskflowid)) {
            return false;
        }
        //Associates the specified value with the specified key in this map.
        // If the map previously contained a mapping for the key, the old value is replaced.
        xftaskflowHashMap.put(taskflowid, xftaskflow);

        return true;
    }

    public boolean deleteXFTaskflow(String taskflowid)
    {
        if (StringUtil.isNullOrEmpty(taskflowid)) {
            return false;
        }
        xftaskflowHashMap.remove(taskflowid);
        return true;
    }

    public boolean deleteManyXFTaskflow(List<String> taskflowIDs)
    {
        if (taskflowIDs == null) {
            return false;
        }

        for(String taskflowId : taskflowIDs)
        {
            deleteXFTaskflow(taskflowId);
        }
        return true;
    }

    public Optional<XFTaskflow> findById(String taskflowId, boolean bReadFromDB, boolean bSyncFromDB)
    {
        if (StringUtil.isNullOrEmpty(taskflowId)) {
            return Optional.empty();
        }

        XFTaskflow xfTaskflowInMem = xftaskflowHashMap.get(taskflowId);
        if (!bReadFromDB) {
            if (xfTaskflowInMem == null)
            {
                return Optional.empty();
            }
            else
            {
                return Optional.of(xfTaskflowInMem);
            }
        }

        if (xfTaskflowInMem == null)
        {
            try {
                Optional<XFTaskflow> xfTaskflowInDBOpt = taskflowRepository.findById(taskflowId);
                if (xfTaskflowInDBOpt.isPresent())
                {
                    if (bSyncFromDB)
                    {
                        this.xftaskflowHashMap.put(xfTaskflowInDBOpt.get().getId(), xfTaskflowInDBOpt.get());
                    }

                    return xfTaskflowInDBOpt;
                }
            }
            catch (Exception e)
            {
                logger.warn("Exception in taskflowRepository.findById for " + taskflowId, e );
            }

            return Optional.empty();
        }
        else
        {
            return Optional.of(xfTaskflowInMem);
        }
    }

    public XFTaskflow getXFTaskflow(String taskflowId)
    {
        Optional<XFTaskflow> opt = this.findById(taskflowId, true, true);
        if (opt.isPresent())
        {
            return opt.get();
        }
        return null;
    }

    public boolean syncFromMemoryToDB(String taskflowid)
    {
        if (StringUtil.isNullOrEmpty(taskflowid)) {
            return false;
        }

        XFTaskflow xfTaskflowInMem = xftaskflowHashMap.get(taskflowid);
        if (xfTaskflowInMem == null)
        {
            return false;
        }
        try {
            this.taskflowRepository.save(xfTaskflowInMem);
        }
        catch (Exception e)
        {
            logger.warn("syncFromMemoryToDB failed for taskflowid " + taskflowid, e);
            return false;
        }

        return true;
    }

    public boolean syncFromDBToMemory(String taskflowid)
    {
        boolean bRet = true;
        if (StringUtil.isNullOrEmpty(taskflowid)) {
            return false;
        }

        XFTaskflow xfTaskflowInDB = null;
        try {
            Optional<XFTaskflow> xfTaskflowInDBOpt = this.taskflowRepository.findById(taskflowid);
            if (xfTaskflowInDBOpt.isPresent())
            {
                xfTaskflowInDB = xfTaskflowInDBOpt.get();
                bRet = this.upsertXFTaskflow_notUpdateDB(taskflowid, xfTaskflowInDB);
            }
            else
            {
                logger.warn("syncFromDBToMemory failed because failure to find xftaskflow in DB for taskflowid " + taskflowid);
                bRet = false;
            }
        }
        catch (Exception e)
        {
            logger.warn("syncFromDBToMemory failed for taskflowId " + taskflowid, e);
            bRet = false;
        }

        return bRet;
    }

    public Set<Map.Entry<String, XFTaskflow>> getAllXFTaskflows()
    {
        return xftaskflowHashMap.entrySet();
    }

    public void performSyncWithDB()
    {
        try
        {
            for (Map.Entry<String, XFTaskflow> entry: xftaskflowHashMap.entrySet()) {
                XFTaskflow aXFTaskflow = entry.getValue();
                if (aXFTaskflow == null ) {
                    continue;
                }

                Optional<XFTaskflow> taskflowInDBOpt = this.taskflowRepository.findById(aXFTaskflow.getId());
                if (taskflowInDBOpt.isPresent() == false)
                {
                    try {
                        this.taskflowRepository.save(aXFTaskflow);
                    }
                    catch (Exception saveEx)
                    {
                        logger.warn("performSyncWithDB taskflowRepository.save failed exception for taskflowId=" + aXFTaskflow.getId() , saveEx);
                    }
                }
                else
                {
                    XFTaskflow taskflowInDB = taskflowInDBOpt.get();
                    if (taskflowInDB.getStatus() <= XFCommon.TASKFLOWSTATUS_CompletedNormally && taskflowInDB.getStatus() < aXFTaskflow.getStatus())
                    {
                        //xftodo, we should only update the status, but we don't have the api now, enhance it later if necessary
                        this.taskflowRepository.save(aXFTaskflow);
                    }
                    else if (aXFTaskflow.getStatus() <= XFCommon.TASKFLOWSTATUS_CompletedNormally && aXFTaskflow.getStatus() < taskflowInDB.getStatus())
                    {
                        this.xftaskflowHashMap.put(aXFTaskflow.getId(), taskflowInDB);
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.warn("performSyncWithDB failed with exception:" , e);
        }
    }


    public List<String> findAllFinishedTaskflowIds()
    {
        List<String> retList = new ArrayList<String>();
        for (Map.Entry<String, XFTaskflow> entry: xftaskflowHashMap.entrySet()) {
            XFTaskflow aXFTaskflow = entry.getValue();
            if (aXFTaskflow == null ) {
                continue;
            }

            if (aXFTaskflow.getStatus() >= XFCommon.TASKFLOWSTATUS_CompletedNormally )
            {
                retList.add(aXFTaskflow.getId());
            }
        }

        return retList;
    }

    /////////////////////////////////////////////////////////////////////////////////
    // below are functions that have the same function name as the XFTaskflowRepository//
    /////////////////////////////////////////////////////////////////////////////////


    // Criteria matchJobId = new Criteria().where("jobId").is(jobIdOrTaskflowId);
    // Criteria matchTaskflowId = new Criteria().where("id").is(jobIdOrTaskflowId);
    // Criteria matchStatus = new Criteria().where("status").lt(XFCommon.TASKFLOWSTATUS_CompletedNormally);
    // Query query = new Query( new Criteria().orOperator(matchJobId, matchTaskflowId).andOperator(matchStatus));
    public List<XFTaskflow> findRunningTaskflowByJobIdOrTaskflowId(String jobIdOrTaskflowId)
    {
        List<XFTaskflow> retList = new ArrayList<XFTaskflow>();
        for (Map.Entry<String, XFTaskflow> entry: xftaskflowHashMap.entrySet()) {
            XFTaskflow aXFTaskflow = entry.getValue();
            if (aXFTaskflow == null ) {
                continue;
            }

            if ( (aXFTaskflow.getJobId().equals(jobIdOrTaskflowId) || aXFTaskflow.getId().equals(jobIdOrTaskflowId))
                    && aXFTaskflow.getStatus() < XFCommon.TASKFLOWSTATUS_CompletedNormally )
            {
                retList.add(aXFTaskflow);
            }
        }

        return retList;
    }

    public List<XFTaskflow> findRunningTaskflowByJobId(String jobId)
    {
        List<XFTaskflow> retList = new ArrayList<XFTaskflow>();
        for (Map.Entry<String, XFTaskflow> entry: xftaskflowHashMap.entrySet()) {
            XFTaskflow aXFTaskflow = entry.getValue();
            if (aXFTaskflow == null ) {
                continue;
            }

            if ( (aXFTaskflow.getJobId().equals(jobId))
                    && aXFTaskflow.getStatus() < XFCommon.TASKFLOWSTATUS_CompletedNormally )
            {
                retList.add(aXFTaskflow);
            }
        }

        return retList;
    }

    public List<XFTaskflow> findRunningTaskflowByTaskflowId(String taskflowId)
    {
        List<XFTaskflow> retList = new ArrayList<XFTaskflow>();
        for (Map.Entry<String, XFTaskflow> entry: xftaskflowHashMap.entrySet()) {
            XFTaskflow aXFTaskflow = entry.getValue();
            if (aXFTaskflow == null ) {
                continue;
            }

            if ( aXFTaskflow.getId().equals(taskflowId)
                    && (aXFTaskflow.getStatus() < XFCommon.TASKFLOWSTATUS_CompletedNormally) )
            {
                retList.add(aXFTaskflow);
            }
        }

        return retList;
    }


    public List<XFTaskflow> findByJobIdAndStatusIn(String jobId, Collection states)
    {
        List<XFTaskflow> flows = new ArrayList<>();
        for (Map.Entry<String, XFTaskflow> entry: xftaskflowHashMap.entrySet()) {
            XFTaskflow aXFTaskflow = entry.getValue();
            if (aXFTaskflow == null) {
                continue;
            }

            if (aXFTaskflow.getJobId().equals(jobId) && states.contains(aXFTaskflow.getStatus())) {
                flows.add(aXFTaskflow);
            }
        }
        return flows;
    }

    public boolean existsByJobId(String jobId)
    {
        boolean bExists = false;
        for (Map.Entry<String, XFTaskflow> entry: xftaskflowHashMap.entrySet()) {
            XFTaskflow aXFTaskflow = entry.getValue();
            if (aXFTaskflow == null ) {
                continue;
            }

            if (aXFTaskflow.getJobId().equals(jobId))
            {
                bExists = true;
                break;
            }
        }

        return bExists;
    }

    //@org.springframework.data.mongodb.repository.Query("{'id':?0}")
    public XFTaskflow findTaskflowById(String taskflowId)
    {
        Optional<XFTaskflow> opt = this.findById(taskflowId, true, true);
        if (opt.isPresent())
        {
            return opt.get();
        }
        else
        {
            return null;
        }
    }
}
