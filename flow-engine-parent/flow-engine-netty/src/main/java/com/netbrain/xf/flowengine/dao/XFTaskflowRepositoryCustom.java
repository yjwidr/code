package com.netbrain.xf.flowengine.dao;

import com.netbrain.xf.model.XFTaskflow;

import java.util.List;

public interface XFTaskflowRepositoryCustom {

    List<XFTaskflow> findRunningTaskflowByJobIdOrTaskflowId(String jobIdOrTaskflowId);

    List<XFTaskflow> findAllUnfinishedTaskflow();

    long countByJobId(String jobId);
}
