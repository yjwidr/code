package com.xxx.autoupdate.apiserver.security.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.xxx.autoupdate.apiserver.exception.CustomerAuthenticationException;
import com.xxx.autoupdate.apiserver.exception.ErrorCodes;
import com.xxx.autoupdate.apiserver.model.UserEntity;
import com.xxx.autoupdate.apiserver.security.model.UserPrincipal;
import com.xxx.autoupdate.apiserver.services.UserService;
@Component
public class CustomUserService implements UserDetailsService {
    @Autowired
    private UserService userService;
    private static final Logger logger = LogManager.getLogger(CustomUserService.class.getName());

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
        logger.info("username={}",username);
        UserDetails userDetails = null;
        UserEntity userEntity = userService.findByUsername(username);
        List<String> authoritiesList = userService.findAuthoritiesByRoleId(userEntity.getRoleId());
        userEntity.setAuthorities(authoritiesList);
        userDetails = new UserPrincipal(userEntity);
        return userDetails;
        }catch(Exception e) {
            throw new  CustomerAuthenticationException(ErrorCodes.ERROR_USERNAME_NOT_EXISTS);
        }
    }
}
