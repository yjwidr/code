package com.netbrain.autoupdate.apiserver.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.netbrain.autoupdate.apiserver.dao.ResourceRepository;
import com.netbrain.autoupdate.apiserver.services.ResourceService;
@Service
public class ResourceServiceImpl implements ResourceService {
	@Autowired
	private ResourceRepository resourceRepository;
	
}
