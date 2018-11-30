package com.netbrain.kc.api.service;

import java.util.List;

import com.netbrain.kc.api.model.datamodel.RoleEntity;
import com.netbrain.kc.api.model.datamodel.parameter.RoleParameter;


public interface RoleService{  
	List<RoleEntity> getAllRoles();
	RoleEntity getById(String id);
	RoleEntity save(RoleParameter roleParameter);
	RoleEntity update(RoleParameter roleParameter);
} 