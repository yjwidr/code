package com.netbrain.autoupdate.apiserver.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.netbrain.autoupdate.apiserver.dao.UserRepository;
import com.netbrain.autoupdate.apiserver.exception.BusinessException;
import com.netbrain.autoupdate.apiserver.exception.ErrorCodes;
import com.netbrain.autoupdate.apiserver.model.PermissionEntity;
import com.netbrain.autoupdate.apiserver.model.UserEntity;
import com.netbrain.autoupdate.apiserver.services.PermissionService;
import com.netbrain.autoupdate.apiserver.services.RolePermissionService;
import com.netbrain.autoupdate.apiserver.services.UserService;
import com.netbrain.autoupdate.apiserver.util.MD5Util;

@Service
@Transactional(rollbackOn=Exception.class)
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RolePermissionService rolePermissionService;
    @Autowired
    private PermissionService permissionService;

    @Override
    @Transactional(TxType.NOT_SUPPORTED)
    public UserEntity findByUsernameAndPassword(String username, String password) {
        UserEntity user = userRepository.findByUsername(username);
        if(ObjectUtils.isEmpty(user)){
             throw new BusinessException(ErrorCodes.ERROR_USERNAME_NOT_EXISTS);
        }else {
            if (MD5Util.verify(password,user.getPassword())) {
                return user;
            } else {
              throw  new BusinessException(ErrorCodes.ERROR_USERNAME_PASSWORD);
            }
        }
    }

    @Override
    @Transactional(value = TxType.NOT_SUPPORTED)
    public UserEntity findById(String userId) {
        Optional<UserEntity> user = userRepository.findById(userId);
        if(user.isPresent()) {
            return user.get();
        }else {
            throw  new BusinessException(ErrorCodes.ERROR_USERID_NOT_EXISTS);
        }
    }

    @Override
    public List<String> findAuthoritiesByRoleId(String roleId) {
        List<String> permissionIds= rolePermissionService.findByRoleId(roleId);
        List<PermissionEntity> listP=permissionService.findAllById(permissionIds);
        List<String> userAuthorties = new ArrayList<>();
        for (PermissionEntity permissionEntity : listP) {
            userAuthorties.add(permissionEntity.getName());
        }
        return userAuthorties;
    }

    @Override
    public UserEntity save(UserEntity entity) {
        entity=userRepository.save(entity);
        return entity;
    }
}
