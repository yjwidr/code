package com.xxx.autoupdate.apiserver.security.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xxx.autoupdate.apiserver.model.UserEntity;
import com.xxx.autoupdate.apiserver.security.service.CustomUserService;
import com.xxx.autoupdate.apiserver.services.UserService;
import com.xxx.autoupdate.apiserver.token.JwtToken;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;
    @Reference
    private UserService userService;
    @Autowired 
    private CustomUserService customUserService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.isEmpty(authorization)) {
            String[] auth = authorization.split(" ");
            if (auth.length == 2) {
                String authType=auth[0].trim(); 
                String tokenOrLicense=auth[1].trim();
                UserEntity user = null;
                if (!StringUtils.isEmpty(authType) && authType.equals("token") && !StringUtils.isEmpty(tokenOrLicense)) {
                    String userId = JwtToken.unsign(tokenOrLicense, String.class);
                    user = userService.findById(userId);
                    String username=user.getUserName();
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        logger.info("authenticated user " + username + ", setting security context");
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
}
