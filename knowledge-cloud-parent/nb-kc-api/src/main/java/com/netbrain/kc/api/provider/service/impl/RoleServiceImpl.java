package com.netbrain.kc.api.provider.service.impl;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.netbrain.kc.api.exception.ApiException;
import com.netbrain.kc.api.exception.ApiErrorCodes;
import com.netbrain.kc.api.model.datamodel.RoleEntity;
import com.netbrain.kc.api.model.datamodel.parameter.RoleParameter;
import com.netbrain.kc.api.provider.repository.RoleRepository;
import com.netbrain.kc.api.service.RoleService;


@Service
@Transactional(rollbackOn=Exception.class)
public class RoleServiceImpl implements RoleService {
	@Autowired
	private RoleRepository roleRepository;

	@Override
	public List<RoleEntity> getAllRoles() {
		return roleRepository.findAll();
	}

	@Override
	
	public RoleEntity save(RoleParameter roleParameter) {
		RoleEntity entity = new RoleEntity();
		entity.setName(roleParameter.getName());
		entity.setDescription(roleParameter.getDescription());
        entity=roleRepository.save(entity);
        return entity;
	}

	@Override
	public RoleEntity update(RoleParameter roleParameter) {
		Optional<RoleEntity> optional=roleRepository.findById(roleParameter.getId());
        if(optional.isPresent()) {
        	RoleEntity role = optional.get();
    		role.setName(roleParameter.getName());
    		role.setDescription(roleParameter.getDescription());
            role=roleRepository.save(role);
        }else {
            throw  new ApiException(ApiErrorCodes.ERROR_ROLEID_NOT_EXISTS);
        }

		return null;
	}
	
	
	
}
