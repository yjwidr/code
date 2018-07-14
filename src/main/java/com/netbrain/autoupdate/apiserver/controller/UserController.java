package com.netbrain.autoupdate.apiserver.controller;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netbrain.autoupdate.apiserver.annotation.Permission;
import com.netbrain.autoupdate.apiserver.dao.RoleRepository;
import com.netbrain.autoupdate.apiserver.dao.UserRepository;
import com.netbrain.autoupdate.apiserver.model.UserEntity;
import com.netbrain.autoupdate.apiserver.services.UserService;
import com.netbrain.autoupdate.apiserver.util.MD5Util;

@RestController
@RequestMapping(value = "/user")
public class UserController {
	private static Logger logger = LogManager.getLogger(UserController.class.getName());

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @Permission
    public List<UserEntity> list() {        
        logger.debug("debug message ===============================");
        return userRepository.findAll();
    }
    
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @Permission
    @Transactional(rollbackOn=Exception.class)
    public UserEntity save() { 
    	Calendar calendar = Calendar.getInstance();
    	//user.setCreateTime(new Timestamp(calendar.getTime().getTime()));
    	UserEntity user =new UserEntity();
        user.setUserName("bbbbb");
        user.setCreateUserId("10000");
        user.setPassword(MD5Util.generate("12345678"));
        user.setLicenceId("11111111");
        user.setRoleId("10000");
        user.setUpdateTime(new Date());
        user.setUpdateUserId("10000");
        user.setLoginName("bbbbb");
        user.setEmail("1@1.com");
        user.setDescription("dddddddd");
        user.setCompany("bbbbbbbbbbbbbbbb");
        user.setCreateTime(new Date());
    	user=userService.save(user);
    	if (3/0 == 1) {} 
        return user;
    } 
}
