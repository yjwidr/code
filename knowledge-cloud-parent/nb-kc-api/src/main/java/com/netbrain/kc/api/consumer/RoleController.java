package com.netbrain.kc.api.consumer;

import java.util.Collection;
import java.util.List;

import javax.validation.Valid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netbrain.kc.api.constant.Constants;
import com.netbrain.kc.api.model.datamodel.RoleEntity;
import com.netbrain.kc.api.model.datamodel.UserEntity;
import com.netbrain.kc.api.model.datamodel.parameter.RoleParameter;
import com.netbrain.kc.api.service.RoleService;
import com.netbrain.kc.framework.util.CommonUtils;
import com.netbrain.kc.framework.util.ResponseEntity;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
@RestController
@RequestMapping(Constants.ROLE)
@Validated
public class RoleController {
    private static Logger logger = LogManager.getLogger(RoleController.class.getName());
    @Autowired
    private RoleService roleService;

    @ApiOperation(value = Constants.LIST)
    @RequestMapping(value = Constants.LIST, method = RequestMethod.GET)
    @PreAuthorize("hasPermission(#targert, '"+Constants.PU+"') and hasPermission(#targert, '"+Constants.PUBLISH+"')")
    public MappingJacksonValue list(@ApiParam(value = Constants.CVIP, required = true) @RequestParam(name=Constants.CVIP,required = true, defaultValue = Constants.EMP) @Length(min=1, max=32, message = Constants.CVI) String cvi){
    	Authentication auth=SecurityContextHolder.getContext().getAuthentication();
    	Collection<? extends GrantedAuthority> authorities=auth.getAuthorities();
    	String userName=((UserEntity)auth.getPrincipal()).getUserName();
    	String id=((UserEntity)auth.getPrincipal()).getId();
    	List<RoleEntity> list=roleService.getAllRoles();
    	//filter filed json
        return CommonUtils.builderJson(RoleEntity.class.getName(),true,list,Constants.NAME,Constants.CT,Constants.UT);
    }
    @ApiOperation(value = Constants.ADD)
    @RequestMapping(value = Constants.ADD, method = RequestMethod.POST)
    @PreAuthorize("hasPermission(#targert, '"+Constants.PU+"') and hasPermission(#targert, '"+Constants.PUBLISH+"')")
    public ResponseEntity add(@RequestBody @Valid RoleParameter roleParameter){
    	roleService.save(roleParameter);
        return ResponseEntity.ok();
    } 
    @ApiOperation(value = Constants.UPDATE)
    @RequestMapping(value = Constants.UPDATE, method = RequestMethod.POST)
    @PreAuthorize("hasPermission(#targert, '"+Constants.PU+"') and hasPermission(#targert, '"+Constants.PUBLISH+"')")
    public ResponseEntity update(@RequestBody @Valid RoleParameter roleParameter){
    	roleService.update(roleParameter);
    	return ResponseEntity.ok();
    } 
}
