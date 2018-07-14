package com.netbrain.autoupdate.apiserver.services;

import java.util.List;

import com.netbrain.autoupdate.apiserver.model.UserEntity;

public interface UserService{
    UserEntity findByUsernameAndPassword(String username,String password) ; 
    UserEntity findById(String userId) ;
    List<String> findAuthoritiesByRoleId(String roleId);
    UserEntity save(UserEntity entity);
}
