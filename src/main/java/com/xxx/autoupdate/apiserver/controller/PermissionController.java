package com.xxx.autoupdate.apiserver.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.xxx.autoupdate.apiserver.model.constant.Constants;
import com.xxx.autoupdate.apiserver.util.ResponseEntity;
import com.xxx.autoupdate.apiserver.util.UserThreadLocal;

@RestController
@RequestMapping(value = Constants.PERMISSION)
public class PermissionController {
	//private static Logger logger = LogManager.getLogger(PermissionController.class.getName());
	
	@RequestMapping(value = Constants.CURRENT, method = RequestMethod.GET)
	public ResponseEntity getPermissionList(){
		return ResponseEntity.ok(UserThreadLocal.get().getAuthorities()) ;
	}
}
