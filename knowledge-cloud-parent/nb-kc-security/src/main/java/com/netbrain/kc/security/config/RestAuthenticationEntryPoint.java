package com.netbrain.kc.security.config;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.netbrain.kc.framework.exception.FrameworkErrorCodes;
import com.netbrain.kc.framework.token.JwtToken;
import com.netbrain.kc.framework.util.ResponseEntity;
import com.netbrain.kc.security.exception.CustomerAuthenticationException;
import com.netbrain.kc.security.exception.TokenExpiredAuthenticationException;
@Configuration
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private Long expirtedTime;
    public RestAuthenticationEntryPoint(Long expirtedTime) {
    	this.expirtedTime=expirtedTime;
	}
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException ) throws IOException {
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/javascript;charset=utf-8");
        CustomerAuthenticationException cae=null;
        TokenExpiredAuthenticationException teae=null;
        String refreshToken=null; 
        if(authException instanceof  CustomerAuthenticationException) {
        	cae=(CustomerAuthenticationException)authException;
        }else if(authException instanceof  TokenExpiredAuthenticationException){
        	teae=(TokenExpiredAuthenticationException)authException;
        	refreshToken=getRefreshToken(request);
        	response.setHeader("token", refreshToken);
        	response.getWriter().write(JSONObject.toJSONString(new ResponseEntity(teae.getCode(),teae.getMessage()),SerializerFeature.PrettyFormat));
        	return;
        }else{
        	cae=new CustomerAuthenticationException(FrameworkErrorCodes.ERROR_AUTH_OTHER.getCode(),authException.getMessage());
        }
        response.getWriter().write(JSONObject.toJSONString(new ResponseEntity(cae.getCode(),cae.getMessage()),SerializerFeature.PrettyFormat));
    }
    private String getRefreshToken(HttpServletRequest request) throws IOException {
        String authorization = request.getHeader("Authorization");
        String token=null;
        String refreshToken=null;
        if (!StringUtils.isEmpty(authorization)) {
          String[] auth = authorization.split(" ");
          if (auth.length == 2) {
              String authType=auth[0].trim(); 
              token=auth[1].trim();
          	}
        }
    	String payload=JwtToken.getPayload(token);
    	refreshToken=JwtToken.sign(payload, expirtedTime);
    	return refreshToken;
    }
}