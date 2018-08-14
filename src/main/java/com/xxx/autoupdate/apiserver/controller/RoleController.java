package com.xxx.autoupdate.apiserver.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.xxx.autoupdate.apiserver.annotation.Permission;
import com.xxx.autoupdate.apiserver.dao.RoleRepository;
import com.xxx.autoupdate.apiserver.model.RoleEntity;

@RestController
@RequestMapping(value = "/role")
public class RoleController {
	private static Logger logger = LogManager.getLogger(RoleController.class.getName());

    
    @Autowired
    private RoleRepository roleRepository;
    
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @Permission
    public List<RoleEntity> list() {        
        logger.debug("get role list");
        return roleRepository.findAll();
    }
    
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @Permission
    public RoleEntity save() {   

        return null;
    } 
}
