package com.xxx.autoupdate.apiserver.security.config;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.autoupdate.apiserver.model.constant.Constants;
import com.xxx.autoupdate.apiserver.security.model.UserPrincipal;
import com.xxx.autoupdate.apiserver.token.JwtToken;
import com.xxx.autoupdate.apiserver.util.ResponseEntity;
 
/**
 * 验证用户名密码正确后，生成一个token，并将token返回给客户端
 * 该类继承自UsernamePasswordAuthenticationFilter，重写了其中的2个方法
 * attemptAuthentication ：接收并解析用户凭证。
 * successfulAuthentication ：用户成功登录后，这个方法会被调用，我们在这个方法里生成token。
 * @author zhaoxinguo on 2017/9/12.
 */
public class JWTLoginFilter extends UsernamePasswordAuthenticationFilter {
	@Value("${token.expired.time}")
    private Long expirtedTime;
    private AuthenticationManager authenticationManager;
 
    public JWTLoginFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
 
    // 接收并解析用户凭证
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
        	ObjectMapper mapper = new ObjectMapper(); 
        	Map result = null; 
        	result = mapper.readValue(request.getReader(), Map.class); 
        	String username = (String)result.get("username"); 
        	String password = (String)result.get("password");
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username,password));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
 
    // 用户成功登录后，这个方法会被调用，我们在这个方法里生成token
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
 
    	String token = JwtToken.sign(((UserPrincipal)auth).getUser().getId(), expirtedTime);
        Map<String,String> map = new HashMap<>();
        map.put(Constants.TOKEN, token);
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/javascript;charset=utf-8");
    	response.addHeader("Authorization", "Bearer " + token);
    	response.getWriter().write(JSONObject.toJSONString( ResponseEntity.ok(map),SerializerFeature.PrettyFormat));
    }
 
}

