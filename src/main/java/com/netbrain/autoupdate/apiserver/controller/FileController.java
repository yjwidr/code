package com.netbrain.autoupdate.apiserver.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotEmpty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.netbrain.autoupdate.apiserver.annotation.Permission;
import com.netbrain.autoupdate.apiserver.dao.FileRepository;
import com.netbrain.autoupdate.apiserver.model.FileEntity;
import com.netbrain.autoupdate.apiserver.model.UserEntity;
import com.netbrain.autoupdate.apiserver.util.ResponseEntity;
@RestController
@RequestMapping(value = "/file")
@Validated
public class FileController {
    private static Logger logger = LogManager.getLogger(FileController.class.getName());

    @Autowired
    private FileRepository fileRepository;
    
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @Permission(authorities= {"Download"})
    public MappingJacksonValue list(@RequestParam(name="sv",required = true, defaultValue = "") @NotEmpty(message = "sv parameter can't empty") String sv) throws Exception{        
        List<FileEntity> list = new ArrayList<FileEntity>();
        FileEntity file =new FileEntity();
        file.setSize(1000l);
        list.add(file);
        Map<String, Object> resultMap = new HashMap<>();
        UserEntity user =new UserEntity();
        user.setId("3333");
        user.setUserName("bbbbb");
        List<String> authorities=new ArrayList<>();
        authorities.add("Upload");
        user.setAuthorities(authorities);
        resultMap.put("list", list);
        resultMap.put("user", user);
        MappingJacksonValue mjv =new MappingJacksonValue(ResponseEntity.ok(resultMap)) ;
//        MappingJacksonValue mjv =new MappingJacksonValue(resultMap) ;
        SimpleFilterProvider filterProvider = getFilter();
        mjv.setFilters(filterProvider);
        if (3/0 == 1) {} 
        return mjv;
    }

	private SimpleFilterProvider getFilter() {
		SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(UserEntity.class.getName(),SimpleBeanPropertyFilter.filterOutAllExcept(new String[] {"authorities"}));
        filterProvider.addFilter(FileEntity.class.getName(),SimpleBeanPropertyFilter.filterOutAllExcept(new String[] {"id","fileName","size","createTime","updateTime"}));
		return filterProvider;
	}

    @RequestMapping(value = "/save", method = RequestMethod.GET)
    public FileEntity save() {
        FileEntity file=fileRepository.save(new FileEntity());
        if (3/0 == 1) {} //test tx rollback
        return file;
    }

    @RequestMapping(value = "/delete", method = RequestMethod.GET)
    public List<FileEntity> delete(String id) {
        fileRepository.deleteById(id);
        return fileRepository.findAll();
    }
}