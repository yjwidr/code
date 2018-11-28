package com.netbrain.kc.security.config;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbrain.kc.api.model.datamodel.UserEntity;
import com.netbrain.kc.framework.model.constant.Constants;
import com.netbrain.kc.framework.token.JwtToken;
import com.netbrain.kc.framework.util.CommonUtils;
import com.netbrain.kc.framework.util.ResponseEntity;

public class JWTLoginFilter extends UsernamePasswordAuthenticationFilter {

    private AuthenticationManager authenticationManager;
    private Long expirtedTime;
    public JWTLoginFilter(AuthenticationManager authenticationManager,Long expirtedTime) {
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/authorization/token", "POST"));
        this.authenticationManager = authenticationManager;
        this.expirtedTime=expirtedTime;
    }
 
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
        	ObjectMapper mapper = new ObjectMapper(); 
        	Map result = mapper.readValue(request.getReader(), Map.class); 
        	String username = (String)result.get("userName"); 
        	String password =CommonUtils.base64decode((String)result.get("password"));
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username,password));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
 
    	String token = JwtToken.sign(((UserEntity)auth.getPrincipal()).getUserName(), expirtedTime);
        Map<String,String> map = new HashMap<>();
        map.put(Constants.TOKEN, token);
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/javascript;charset=utf-8");
    	response.addHeader("Authorization", "token " + token);
    	response.getWriter().write(JSONObject.toJSONString( ResponseEntity.ok(map),SerializerFeature.PrettyFormat));
    }
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
            HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(ResponseEntity.usernameOrPasswordIncorrect(),SerializerFeature.PrettyFormat));
    }
 
}

