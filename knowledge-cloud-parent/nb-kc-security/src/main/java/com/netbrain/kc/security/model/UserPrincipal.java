package com.netbrain.kc.security.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.netbrain.kc.api.model.datamodel.UserEntity;



public class UserPrincipal implements UserDetails {
    private static final long serialVersionUID = 1L;
    private UserEntity user;
 
    public UserPrincipal(UserEntity user) {
        this.user = user;
    }
 
    public UserEntity getUser() {
        return user;
    }
    
    @Override
    public String getUsername() {
        return user.getUserName();
    }
 

    @Override
    public String getPassword() {
        return user.getPassword();
    }
 
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();
        for (String string : user.getAuthorities()) {
            authList.add(new SimpleGrantedAuthority(string));
        }
        return authList;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}