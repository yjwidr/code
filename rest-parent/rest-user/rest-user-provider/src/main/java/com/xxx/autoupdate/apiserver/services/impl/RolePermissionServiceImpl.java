package com.xxx.autoupdate.apiserver.services.impl;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xxx.autoupdate.apiserver.dao.RolePermissionRepository;
import com.xxx.autoupdate.apiserver.exception.BusinessException;
import com.xxx.autoupdate.apiserver.exception.ErrorCodes;
import com.xxx.autoupdate.apiserver.services.RolePermissionService;
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
            throw new BusinessException(ErrorCodes.ERROR_ROLEID_NOT_EXISTS);
        }
        return list ;
    }

}
