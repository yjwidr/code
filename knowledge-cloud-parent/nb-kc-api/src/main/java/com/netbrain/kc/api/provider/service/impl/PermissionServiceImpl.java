package com.netbrain.kc.api.provider.service.impl;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.netbrain.kc.api.exception.ApiException;
import com.netbrain.kc.api.exception.ApiErrorCodes;
import com.netbrain.kc.api.model.datamodel.PermissionEntity;
import com.netbrain.kc.api.provider.repository.PermissionRepository;
import com.netbrain.kc.api.service.PermissionService;

@Service
@Transactional(rollbackOn=Exception.class)
public class PermissionServiceImpl implements PermissionService {
    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    @Transactional(TxType.NOT_SUPPORTED)
    public List<PermissionEntity> findAllById(List<String> permissionIds) {
        List<PermissionEntity> list=permissionRepository.findAllById(permissionIds);
        if(list==null || list.size()==0) {
            throw new ApiException(ApiErrorCodes.ERROR_PERMISSIONID_NOT_EXISTS);
        }
        return list ;
    } 
}
