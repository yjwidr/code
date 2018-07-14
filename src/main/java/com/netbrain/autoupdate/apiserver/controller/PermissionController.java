package com.netbrain.autoupdate.apiserver.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netbrain.autoupdate.apiserver.util.ResponseEntity;
import com.netbrain.autoupdate.apiserver.util.UserThreadLocal;

@RestController
@RequestMapping(value = "permission")
public class PermissionController {
	private static Logger logger = LogManager.getLogger(PermissionController.class.getName());
	
	@RequestMapping(value = "current", method = RequestMethod.GET)
	public ResponseEntity getPermissionList(){
		return ResponseEntity.ok(UserThreadLocal.get().getAuthorities()) ;
	}
}
