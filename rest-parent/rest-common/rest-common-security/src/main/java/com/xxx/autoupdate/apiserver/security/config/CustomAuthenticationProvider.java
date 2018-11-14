package com.xxx.autoupdate.apiserver.security.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.xxx.autoupdate.apiserver.exception.ErrorCodes;
import com.xxx.autoupdate.apiserver.security.model.UserPrincipal;

public class CustomAuthenticationProvider implements AuthenticationProvider {

    private UserDetailsService userDetailsService;

    private MD5Password md5Password;

    public CustomAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder){
        this.userDetailsService = userDetailsService;
        this.md5Password = (MD5Password) passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();
        UserPrincipal principal =(UserPrincipal)userDetailsService.loadUserByUsername(name);
        if (md5Password.matches(password, principal.getPassword())) {
            Authentication auth = new UsernamePasswordAuthenticationToken(principal.getUser(), password, principal.getAuthorities());
            return auth;
        } else {
            throw new BadCredentialsException(ErrorCodes.ERROR_USERNAME_PASSWORD.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

}
