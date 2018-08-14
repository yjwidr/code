package com.xxx.autoupdate.apiserver.services;

import java.util.List;

import com.xxx.autoupdate.apiserver.model.ResourceEntity;

public interface ResourceService {
	List<ResourceEntity> getResourcesByPackageId(String packageId);
}
