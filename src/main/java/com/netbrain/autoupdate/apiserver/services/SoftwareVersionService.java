package com.netbrain.autoupdate.apiserver.services;

import java.util.List;

import com.netbrain.autoupdate.apiserver.model.SoftwareVersionEntity;

public interface SoftwareVersionService {
	List<SoftwareVersionEntity> getSoftwareVersionList();
}
