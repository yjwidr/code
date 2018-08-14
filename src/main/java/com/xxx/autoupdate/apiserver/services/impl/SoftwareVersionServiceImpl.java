package com.xxx.autoupdate.apiserver.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.xxx.autoupdate.apiserver.dao.SoftwareVersionRepository;
import com.xxx.autoupdate.apiserver.model.SoftwareVersionEntity;
import com.xxx.autoupdate.apiserver.services.SoftwareVersionService;
@Service
public class SoftwareVersionServiceImpl implements SoftwareVersionService {
	@Autowired
	private SoftwareVersionRepository softwareVersionRepository;
	
	@Override
	public List<SoftwareVersionEntity> getSoftwareVersionList() {
		Sort sort = new Sort(Sort.Direction.DESC, "createTime");
		return softwareVersionRepository.findAll(sort);
	}

}
