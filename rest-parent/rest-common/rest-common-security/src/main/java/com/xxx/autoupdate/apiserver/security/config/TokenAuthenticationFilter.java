package com.xxx.autoupdate.apiserver.security.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xxx.autoupdate.apiserver.model.UserEntity;
import com.xxx.autoupdate.apiserver.services.UserService;
import com.xxx.autoupdate.apiserver.token.JwtToken;

public class TokenAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    private static final String BEARER = "Bearer";
//    @Autowired
//    private UserDetailsService userDetailsService;
//    @Reference
//    private UserService userService;
    protected TokenAuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)throws AuthenticationException, IOException, ServletException {
        String authorization = request.getHeader("Authorization");
        
        if (!StringUtils.isEmpty(authorization)) {
            String[] auth = authorization.split(" ");
            if (auth.length == 2) {
                String authType=auth[0].trim(); 
                String tokenOrLicense=auth[1].trim();
                UserEntity user = null;
                if (!StringUtils.isEmpty(authType) && authType.equals("token") && !StringUtils.isEmpty(tokenOrLicense)) {
                    String userId = JwtToken.unsign(tokenOrLicense, String.class);
//                    user = userService.findById(userId);
//                    String username=user.getUserName();
//                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        if (SecurityContextHolder.getContext().getAuthentication() == null) {
//                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("ttt", null, null);
                        authentication.setDetails(new WebAuthenticationDetails(request));
//                        logger.info("authenticated user " + username + ", setting security context");
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        return authentication;
                    }else if (SecurityContextHolder.getContext().getAuthentication() != null) {
                        return SecurityContextHolder.getContext().getAuthentication();
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void successfulAuthentication(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain chain, final Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        chain.doFilter(request, response);
    }
}
