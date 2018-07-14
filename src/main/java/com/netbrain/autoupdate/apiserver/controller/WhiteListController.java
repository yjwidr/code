package com.netbrain.autoupdate.apiserver.controller;

import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.netbrain.autoupdate.apiserver.annotation.Permission;
import com.netbrain.autoupdate.apiserver.model.SoftwareVersionEntity;
import com.netbrain.autoupdate.apiserver.model.parameter.ContentVersionIds;
import com.netbrain.autoupdate.apiserver.model.parameter.WhiteListUser;
import com.netbrain.autoupdate.apiserver.services.WhiteListService;
import com.netbrain.autoupdate.apiserver.util.ResponseEntity;
@RestController
@RequestMapping("whiteList")
public class WhiteListController {
    private static Logger logger = LogManager.getLogger(WhiteListController.class.getName());
    @Autowired
    private WhiteListService whiteListService;

    @RequestMapping(value = "list", method = RequestMethod.GET)
    @Permission(authorities={"Push Upgrade"})
    public ResponseEntity list(@RequestParam(name="cvi",required = true, defaultValue = "") @Length(min=1, max=32, message = "cvi parameter can't empty") String cvi){
        List<WhiteListUser> list=whiteListService.getWhiteList(cvi);
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(SoftwareVersionEntity.class.getName(),SimpleBeanPropertyFilter.filterOutAllExcept(new String[] {"version","name","description","createTime","updateTime"}));
        MappingJacksonValue jacksonValue =new MappingJacksonValue(ResponseEntity.ok(list)) ;
        jacksonValue.setFilters(filterProvider);
        return ResponseEntity.ok(list);
    }
    @RequestMapping(value = "add", method = RequestMethod.POST)
    @Permission(authorities={"Push Upgrade"})
    public ResponseEntity inventory(@RequestBody @Valid WhiteListUser whiteListUser){
        whiteListUser=whiteListService.AddWhiteList(whiteListUser);
        return ResponseEntity.ok(whiteListUser);
    }    
    @RequestMapping(value = "update", method = RequestMethod.POST)
    @Permission(authorities={"Push Upgrade"})
    public ResponseEntity update(@RequestBody @Valid WhiteListUser whiteListUser){
        whiteListService.UpdateWhiteList(whiteListUser);
        return ResponseEntity.ok();
    }    
    @RequestMapping(value = "delete", method = RequestMethod.POST)
    @Permission(authorities={"Push Upgrade"})
    public ResponseEntity delete(@RequestBody @Valid ContentVersionIds contentVersionIds ){
        whiteListService.DeleteWhiteList(Arrays.asList(contentVersionIds.getContentVersionIds()));
        return ResponseEntity.ok();
    }
}
