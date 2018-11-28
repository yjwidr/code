package com.xxx.autoupdate.apiserver.controller;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xxx.autoupdate.apiserver.model.UserEntity;
import com.xxx.autoupdate.apiserver.model.constant.Constants;
import com.xxx.autoupdate.apiserver.model.parameter.LoginUser;
import com.xxx.autoupdate.apiserver.services.UserService;
import com.xxx.autoupdate.apiserver.token.JwtToken;
import com.xxx.autoupdate.apiserver.util.CommonUtils;
import com.xxx.autoupdate.apiserver.util.ResponseEntity;
@RestController
@RequestMapping(Constants.AUTH)
public class LoginController {
    private static Logger logger = LogManager.getLogger(LoginController.class.getName());
    @Reference
    private UserService userService;
    @Value("${token.expired.time}")
    private Long expirtedTime;

    @RequestMapping(value = Constants.TOKEN, method = RequestMethod.POST,consumes =MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity login(@RequestBody @Valid LoginUser loginUser) throws UnsupportedEncodingException, JsonProcessingException {
        UserEntity user = userService.findByUsernameAndPassword(loginUser.getUserName(), CommonUtils.base64decode(loginUser.getPassword()));
        String token = JwtToken.sign(user.getId(), expirtedTime);
        Map<String,String> map = new HashMap<>();
        map.put(Constants.TOKEN, token);
        return ResponseEntity.ok(map);
    }
}
