package com.xxx.autoupdate.apiserver.services.impl;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xxx.autoupdate.apiserver.dao.PermissionRepository;
import com.xxx.autoupdate.apiserver.exception.BusinessException;
import com.xxx.autoupdate.apiserver.exception.ErrorCodes;
import com.xxx.autoupdate.apiserver.model.PermissionEntity;
import com.xxx.autoupdate.apiserver.services.PermissionService;
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
            throw new BusinessException(ErrorCodes.ERROR_PERMISSIONID_NOT_EXISTS);
        }
        return list ;
    } 
}
