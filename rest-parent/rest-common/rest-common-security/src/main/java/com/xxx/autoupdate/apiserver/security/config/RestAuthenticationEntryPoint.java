package com.xxx.autoupdate.apiserver.security.config;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.xxx.autoupdate.apiserver.exception.CustomerAuthenticationException;
import com.xxx.autoupdate.apiserver.exception.ErrorCodes;
import com.xxx.autoupdate.apiserver.util.ResponseEntity;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException ) throws IOException {
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/javascript;charset=utf-8");
        CustomerAuthenticationException cae=null;
        if(authException instanceof  CustomerAuthenticationException) {
        	cae=(CustomerAuthenticationException)authException;
        }else {
        	cae=new CustomerAuthenticationException(ErrorCodes.ERROR_AUTH_OTHER,authException.getMessage());
        }
        response.getWriter().write(JSONObject.toJSONString(new ResponseEntity(cae.getCode(),cae.getMessage()),SerializerFeature.PrettyFormat));
    }
}