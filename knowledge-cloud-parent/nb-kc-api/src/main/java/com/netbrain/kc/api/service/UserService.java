package com.netbrain.kc.api.service;

import java.util.List;

import com.netbrain.kc.api.model.datamodel.UserEntity;


public interface UserService{
    UserEntity findByUsernameAndPassword(String username,String password) ; 
    UserEntity findByUsername(String username) ; 
    UserEntity findById(String userId) ;
    List<String> findAuthoritiesByRoleId(String roleId);
    List<UserEntity> getList();
    UserEntity save(UserEntity entity);
}
