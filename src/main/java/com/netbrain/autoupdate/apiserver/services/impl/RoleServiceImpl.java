package com.netbrain.autoupdate.apiserver.services.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.netbrain.autoupdate.apiserver.dao.RoleRepository;
import com.netbrain.autoupdate.apiserver.model.RoleEntity;
import com.netbrain.autoupdate.apiserver.services.RoleService;
@Service
public class RoleServiceImpl implements RoleService {
	@Autowired
	private RoleRepository roleRepository;

	@Override
	public List<RoleEntity> getAllRoles() {
		return roleRepository.findAll();
	}
	
	
	
}
