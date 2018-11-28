package com.netbrain.kc.api.provider.service.impl;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.netbrain.kc.api.exception.ApiException;
import com.netbrain.kc.api.exception.ApiErrorCodes;
import com.netbrain.kc.api.provider.repository.RolePermissionRepository;
import com.netbrain.kc.api.service.RolePermissionService;


@Service
@Transactional(rollbackOn=Exception.class)
public class RolePermissionServiceImpl implements RolePermissionService {
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    
    @Override
    @Transactional(TxType.NOT_SUPPORTED)
    public List<String> findByRoleId(String roleId) {
        List<String> list=rolePermissionRepository.findByRoleId(roleId);
        if(list==null || list.size()==0) {
            throw new ApiException(ApiErrorCodes.ERROR_ROLEID_NOT_EXISTS);
        }
        return list ;
    }

}
