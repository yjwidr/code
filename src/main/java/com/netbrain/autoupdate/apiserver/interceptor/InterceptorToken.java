package com.netbrain.autoupdate.apiserver.interceptor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.netbrain.autoupdate.apiserver.exception.BusinessException;
import com.netbrain.autoupdate.apiserver.model.UserEntity;
import com.netbrain.autoupdate.apiserver.services.UserService;
import com.netbrain.autoupdate.apiserver.token.JwtToken;
import com.netbrain.autoupdate.apiserver.util.CommonUtils;
import com.netbrain.autoupdate.apiserver.util.ResponseEntity;
import com.netbrain.autoupdate.apiserver.util.UserThreadLocal;

/**
 * Token Interceptor
 */
public class InterceptorToken implements HandlerInterceptor {

    @Autowired
    private UserService userService;
    @Value("${role.id.for.license}")
    private String roleIdforLicense;
    private static Logger logger = LogManager.getLogger(InterceptorToken.class.getName());

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {
        String authorization = request.getHeader("Authorization");
        ResponseEntity responseData = ResponseEntity.ok();
        if (!StringUtils.isEmpty(authorization)) {
            String[] auth = authorization.split(" ");
            if (auth.length == 2) {
                String authType=auth[0].trim();
                String tokenOrLicense=auth[1].trim();
                UserEntity user = null;
                List<String> authoritiesList = null;
                if (!StringUtils.isEmpty(authType) && authType.equals("token") && !StringUtils.isEmpty(tokenOrLicense)) {
                    String userId = JwtToken.unsign(tokenOrLicense, String.class);
                    user = userService.findById(userId);
                    authoritiesList= userService.findAuthoritiesByRoleId(user.getRoleId());
                } else if (!StringUtils.isEmpty(authType) && authType.equals("license") && !StringUtils.isEmpty(tokenOrLicense)) {
                    String license= CommonUtils.base64decode(roleIdforLicense);
                    user =new UserEntity();
                    authoritiesList = userService.findAuthoritiesByRoleId(roleIdforLicense);
                    user.setLicenceId(license);
                }
                user.setAuthorities(authoritiesList);
                UserThreadLocal.set(user);
                logger.info("token={},authType={}",tokenOrLicense,authType);
                return true;
            }
        }else {
            return true;
        }
        responseData = ResponseEntity.unauthorized();
        throw new BusinessException(responseData.getResultCode(),responseData.getErrorMsg());
    }

    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o,
            ModelAndView modelAndView) throws Exception {

    }

    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            Object o, Exception e) throws Exception {
        UserThreadLocal.set(null);
    }
}
