package com.netbrain.kc.api.consumer;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.netbrain.kc.api.constant.Constants;
import com.netbrain.kc.api.model.datamodel.RoleEntity;
import com.netbrain.kc.api.model.datamodel.UserEntity;
import com.netbrain.kc.api.service.RoleService;
import com.netbrain.kc.api.service.UserService;
import com.netbrain.kc.framework.util.CommonUtils;
import com.netbrain.kc.framework.util.ResponseEntity;

import io.swagger.annotations.ApiOperation;
@RestController
@RequestMapping(Constants.USER)
@Validated
public class UserController {
    private static Logger logger = LogManager.getLogger(UserController.class.getName());
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

    @ApiOperation(value = Constants.LIST)
    @RequestMapping(value = Constants.LIST, method = RequestMethod.GET)
    @PreAuthorize("hasPermission(#targert, '"+Constants.PU+"') and hasPermission(#targert, '"+Constants.PUBLISH+"')")
    public MappingJacksonValue list(){
    	Authentication auth=SecurityContextHolder.getContext().getAuthentication();
    	Collection<? extends GrantedAuthority> authorities=auth.getAuthorities();
    	String userName=((UserEntity)auth.getPrincipal()).getUserName();
    	String id=((UserEntity)auth.getPrincipal()).getId();
    	String roleId=((UserEntity)auth.getPrincipal()).getRoleId();
    	List<UserEntity> list=userService.getList();
    	for (UserEntity userEntity : list) {
    		List<String> permissions= userService.findAuthoritiesByRoleId(roleId);
    		userEntity.setAuthorities(permissions);
    		RoleEntity roleEntity= roleService.getById(roleId);
    		userEntity.setRoleEntity(roleEntity);
		}
    	//filter filed json
    	SimpleFilterProvider filterProvider = new SimpleFilterProvider();
    	CommonUtils.builderJsonFilter(filterProvider,RoleEntity.class.getName(),false,Constants.NAME);
    	CommonUtils.builderJsonFilter(filterProvider,UserEntity.class.getName(),false,Constants.LOGINNAME,Constants.CT,Constants.UT,Constants.ROLEENTITY);
    	MappingJacksonValue jacksonValue =new MappingJacksonValue(ResponseEntity.ok(list)) ;
        jacksonValue.setFilters(filterProvider);
        return jacksonValue;
    }
}
