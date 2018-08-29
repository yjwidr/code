package com.xxx.autoupdate.apiserver.security.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xxx.autoupdate.apiserver.model.UserEntity;
import com.xxx.autoupdate.apiserver.services.UserService;
@Component
public class CustomUserService implements UserDetailsService {
    @Reference
    private UserService userService;
    private static final Logger logger = LogManager.getLogger(CustomUserService.class.getName());

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("username={}",username);
        UserDetails userDetails = null;
        UserEntity userEntity = userService.findByUsername(username);
        List<String> authoritiesList = userService.findAuthoritiesByRoleId(userEntity.getRoleId());
        List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();
        for (String string : authoritiesList) {
            authList.add(new SimpleGrantedAuthority(string));
        }
        userDetails = new User(userEntity.getUserName(), userEntity.getPassword().toLowerCase(), true, true, true, true, authList);
        return userDetails;
    }
}
