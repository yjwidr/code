package com.xxx.autoupdate.apiserver.controller;

import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.xxx.autoupdate.apiserver.annotation.Permission;
import com.xxx.autoupdate.apiserver.model.SoftwareVersionEntity;
import com.xxx.autoupdate.apiserver.model.constant.Constants;
import com.xxx.autoupdate.apiserver.model.parameter.WhiteListUser;
import com.xxx.autoupdate.apiserver.model.parameter.WhiteUserIds;
import com.xxx.autoupdate.apiserver.services.WhiteListService;
import com.xxx.autoupdate.apiserver.util.ResponseEntity;
@RestController
@RequestMapping(Constants.WL)
public class WhiteListController {
    //private static Logger logger = LogManager.getLogger(WhiteListController.class.getName());
    @Reference
    private WhiteListService whiteListService; 

    @RequestMapping(value = Constants.LIST, method = RequestMethod.GET)
//    @Permission(authorities={Constants.PU})
    @PreAuthorize("hasPermission(#a, '"+Constants.PU+"1')")
    public ResponseEntity list(@RequestParam(name=Constants.CVIP,required = true, defaultValue = Constants.EMP) @Length(min=1, max=32, message = Constants.CVI) String cvi){
        List<WhiteListUser> list=whiteListService.getWhiteList(cvi);
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(SoftwareVersionEntity.class.getName(),SimpleBeanPropertyFilter.filterOutAllExcept(new String[] {Constants.VER,Constants.NAME,Constants.DESC,Constants.CT,Constants.UT}));
        MappingJacksonValue jacksonValue =new MappingJacksonValue(ResponseEntity.ok(list)) ;
        jacksonValue.setFilters(filterProvider);
        return ResponseEntity.ok(list);
    }
    @RequestMapping(value = Constants.ADD, method = RequestMethod.POST)
    @Permission(authorities={Constants.PU})
    public ResponseEntity inventory(@RequestBody @Valid WhiteListUser whiteListUser){
        whiteListUser=whiteListService.AddWhiteList(whiteListUser);
        return ResponseEntity.ok(whiteListUser);
    }    
    @RequestMapping(value = Constants.UPDATE, method = RequestMethod.POST)
    @Permission(authorities={Constants.PU})
    public ResponseEntity update(@RequestBody @Valid WhiteListUser whiteListUser){
        whiteListService.UpdateWhiteList(whiteListUser);
        return ResponseEntity.ok();
    }    
    @RequestMapping(value = Constants.DELETE, method = RequestMethod.POST)
    @Permission(authorities={Constants.PU})
    public ResponseEntity delete(@RequestBody @Valid WhiteUserIds whiteUserIds ){
        whiteListService.DeleteWhiteList(Arrays.asList(whiteUserIds.getWhiteUserIds()));
        return ResponseEntity.ok();
    }
}
