package com.xxx.autoupdate.apiserver.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xxx.autoupdate.apiserver.dao.ResourceRepository;
import com.xxx.autoupdate.apiserver.model.ResourceEntity;
import com.xxx.autoupdate.apiserver.services.ResourceService;
@Service
public class ResourceServiceImpl implements ResourceService {
	@Autowired
	private ResourceRepository resourceRepository;

	@Override
	public List<ResourceEntity> getResourcesByPackageId(String packageId) {
		return resourceRepository.getResourcesByPackageId(packageId);
	}
	
}
