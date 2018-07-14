package com.netbrain.autoupdate.apiserver.controller;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netbrain.autoupdate.apiserver.model.UserEntity;
import com.netbrain.autoupdate.apiserver.model.parameter.LoginUser;
import com.netbrain.autoupdate.apiserver.services.UserService;
import com.netbrain.autoupdate.apiserver.token.JwtToken;
import com.netbrain.autoupdate.apiserver.util.CommonUtils;
import com.netbrain.autoupdate.apiserver.util.ResponseEntity;
@RestController
@RequestMapping("authorization")
public class LoginController {
    private static Logger logger = LogManager.getLogger(LoginController.class.getName());
    @Autowired
    private UserService userService;
    @Value("${token.expired.time}")
    private Long expirtedTime;

    @RequestMapping(value = "token", method = RequestMethod.POST,consumes =MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity login(@RequestBody @Valid LoginUser loginUser) throws UnsupportedEncodingException, JsonProcessingException {
        UserEntity user = userService.findByUsernameAndPassword(loginUser.getUserName(), CommonUtils.base64decode(loginUser.getPassword()));
        String token = JwtToken.sign(user.getId(), expirtedTime);
        Map<String,String> map = new HashMap<>();
        map.put("token", token);
        return ResponseEntity.ok(map);
    }
}
