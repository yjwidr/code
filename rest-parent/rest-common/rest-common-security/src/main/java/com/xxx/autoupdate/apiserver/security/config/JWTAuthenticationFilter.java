package com.xxx.autoupdate.apiserver.security.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import com.xxx.autoupdate.apiserver.model.UserEntity;
import com.xxx.autoupdate.apiserver.security.model.UserPrincipal;
import com.xxx.autoupdate.apiserver.token.JwtToken;
 
/**
 * token的校验
 * 该类继承自BasicAuthenticationFilter，在doFilterInternal方法中，
 * 从http头的Authorization 项读取token数据，然后用Jwts包提供的方法校验token的合法性。
 * 如果校验通过，就认为这是一个取得授权的合法请求
 * @author zhaoxinguo on 2017/9/13.
 */
public class JWTAuthenticationFilter extends BasicAuthenticationFilter {
    private UserDetailsService userDetailsService;
    public JWTAuthenticationFilter(AuthenticationManager authenticationManager,UserDetailsService userDetailsService) {
        super(authenticationManager);
        this.userDetailsService=userDetailsService;
    }
 
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = request.getHeader("Authorization");
 
        if (token == null || !token.startsWith("token ")) {
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
                      String username = JwtToken.unsign(tokenOrLicense, String.class);
                      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                          UserPrincipal principal =(UserPrincipal)userDetailsService.loadUserByUsername(username);
                          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal.getUser(), null, principal.getAuthorities());
                          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                          logger.info("authenticated user " + username + ", setting security context");
                          SecurityContextHolder.getContext().setAuthentication(authentication);
                          return authentication;
                      }
                  }
              }
          }
		return null;
    }
 
}
