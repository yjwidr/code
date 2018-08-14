package com.xxx.autoupdate.apiserver.services;

import java.util.List;

import com.xxx.autoupdate.apiserver.model.SoftwareVersionEntity;

public interface SoftwareVersionService {
	List<SoftwareVersionEntity> getSoftwareVersionList();
}
