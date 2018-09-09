package com.xxx.autoupdate.apiserver.security.config;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xxx.autoupdate.apiserver.model.UserEntity;
import com.xxx.autoupdate.apiserver.services.UserService;
import com.xxx.autoupdate.apiserver.token.JwtToken;
 
/**
 * token的校验
 * 该类继承自BasicAuthenticationFilter，在doFilterInternal方法中，
 * 从http头的Authorization 项读取token数据，然后用Jwts包提供的方法校验token的合法性。
 * 如果校验通过，就认为这是一个取得授权的合法请求
 * @author zhaoxinguo on 2017/9/13.
 */
public class JWTAuthenticationFilter extends BasicAuthenticationFilter {
    @Autowired
    private UserDetailsService userDetailsService;
    @Reference
    private UserService userService; 
    public JWTAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }
 
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = request.getHeader("Authorization");
 
        if (token == null || !token.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
 
        UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
 
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
 
    }
 
    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) throws IOException {
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
                          UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                          logger.info("authenticated user " + username + ", setting security context");
                          return authentication;
                      }
                  }
              }
          }
		return null;
    }
 
}
