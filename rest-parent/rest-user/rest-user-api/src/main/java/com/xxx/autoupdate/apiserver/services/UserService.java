package com.xxx.autoupdate.apiserver.services;

import java.util.List;

import com.xxx.autoupdate.apiserver.model.UserEntity;

public interface UserService{
    UserEntity findByUsernameAndPassword(String username,String password) ; 
    UserEntity findById(String userId) ;
    List<String> findAuthoritiesByRoleId(String roleId);
    UserEntity save(UserEntity entity);
}
