package com.netbrain.xf.flowengine.dao;


import com.netbrain.xf.model.XFDtg;

import java.util.List;

public interface XFDtgRepositoryCustom {

    boolean incTriggerReceivedTotalTimes(XFDtg xfDtg, int times);

    boolean updateFinalTriggerReceived(XFDtg xfDtg, boolean finalTrigger);

    boolean updateDtgStatusByJobIdOrTaskflowId(String taskflowId, int newDtgStatus);

    boolean deleteByJobIdOrTaskflowId(String strJobIdOrTaskflowId);

    boolean stopDtgsByJobIdOrTaskflowId(String strJobIdOrTaskflowId);

    List<XFDtg> findDtgsByJobIdOrTaskflowId(String strJobIdOrTaskflowId);

    List<XFDtg> findDtgsByJobId(String strJobId);
    List<XFDtg> findDtgsByTaskflowId(String strTaskflowId);

    void dropCollection();
}
