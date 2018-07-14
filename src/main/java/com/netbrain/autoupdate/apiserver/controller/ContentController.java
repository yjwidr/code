package com.netbrain.autoupdate.apiserver.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.netbrain.autoupdate.apiserver.annotation.Permission;
import com.netbrain.autoupdate.apiserver.model.ContentPackageEntity;
import com.netbrain.autoupdate.apiserver.model.ContentVersionEntity;
import com.netbrain.autoupdate.apiserver.model.SoftwareVersionEntity;
import com.netbrain.autoupdate.apiserver.model.parameter.ContentVersionIds;
import com.netbrain.autoupdate.apiserver.model.parameter.PackageVersionParameter;
import com.netbrain.autoupdate.apiserver.model.parameter.UploadContent;
import com.netbrain.autoupdate.apiserver.services.ContentVersionService;
import com.netbrain.autoupdate.apiserver.services.SoftwareVersionService;
import com.netbrain.autoupdate.apiserver.util.CommonUtils;
import com.netbrain.autoupdate.apiserver.util.ResponseEntity;
import com.netbrain.autoupdate.apiserver.util.UserThreadLocal;

@RestController
@RequestMapping(value = "content")
@Validated
public class ContentController {
    private static Logger logger = LogManager.getLogger(ContentController.class.getName());
    
    @Autowired
    private SoftwareVersionService softwareVersionService;
    @Autowired
    private ContentVersionService contentVersionService;
    
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public MappingJacksonValue list(){
        List<ContentVersionEntity> list=contentVersionService.getAllContentVersionList();
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(ContentVersionEntity.class.getName(),SimpleBeanPropertyFilter.filterOutAllExcept(
                new String[] {"id","name","type","softwareVersion","contentPackageVersion","publishStatus","activateStatus","author","description","createTime","updateTime"}));
        MappingJacksonValue jacksonValue =new MappingJacksonValue(ResponseEntity.ok(list)) ;
        jacksonValue.setFilters(filterProvider);
        return jacksonValue;
    }
    @RequestMapping(value = "softewareVersionlist", method = RequestMethod.GET)
    public MappingJacksonValue getSoftwareVersionList(){
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(SoftwareVersionEntity.class.getName(),SimpleBeanPropertyFilter.filterOutAllExcept(new String[] {"version","name","description","createTime","updateTime"}));
        MappingJacksonValue jacksonValue =new MappingJacksonValue(ResponseEntity.ok(softwareVersionService.getSoftwareVersionList())) ;
        jacksonValue.setFilters(filterProvider);
        return jacksonValue;
    }
    @RequestMapping(value = "inventory", method = RequestMethod.POST)
    @Permission(authorities={"Upload"})
    public MappingJacksonValue inventory(@RequestBody @Valid PackageVersionParameter packageVersionParameter){
        packageVersionParameter.getBase64Data();
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(SoftwareVersionEntity.class.getName(),SimpleBeanPropertyFilter.filterOutAllExcept(new String[] {"version","name","description","createTime","updateTime"}));
        MappingJacksonValue jacksonValue =new MappingJacksonValue(ResponseEntity.ok(softwareVersionService.getSoftwareVersionList())) ;
        jacksonValue.setFilters(filterProvider);
        return jacksonValue;
    }
    @RequestMapping(value = "lastMinorVersion", method = RequestMethod.GET)
    public ResponseEntity lastMinorVersion(@RequestParam(name="sv",required = true, defaultValue = "") @Length(min=1, max=32, message = "sv parameter can't empty") String sv){
        int[] cv=contentVersionService.getMaxContentMinorVersion(sv);
        if(cv.length==2) {
            Map<String,Integer> map = new HashMap<>();
            map.put("major", Integer.valueOf(cv[0])); 
            map.put("minor", Integer.valueOf(cv[1]));
            return ResponseEntity.ok(map);
        }else {
            return ResponseEntity.ok();
        }
    }
    @RequestMapping(value = "add", method = RequestMethod.POST)
    @Permission(authorities={"Upload"})
    public ResponseEntity add(@Valid UploadContent uploadContent){
        return ResponseEntity.ok(contentVersionService.uploadContentVersionForMultiSoftwareVersion(uploadContent));
    }
    @RequestMapping(value = "publish", method = RequestMethod.POST)
    @Permission(authorities={"Publish"})
    public ResponseEntity publish(@RequestBody @Valid ContentVersionIds contentVersionIds ){
        int count=contentVersionService.publishContentVersion(contentVersionIds.getContentVersionIds(), UserThreadLocal.get().getId());
        return ResponseEntity.ok();
    }
    @RequestMapping(value = "disable", method = RequestMethod.POST)
    @Permission(authorities={"Disable"})
    public ResponseEntity disable(@RequestBody @Valid ContentVersionIds contentVersionIds ){
        int count=contentVersionService.disableContentVersion(contentVersionIds.getContentVersionIds(), UserThreadLocal.get().getId());
        return ResponseEntity.ok();
    }
    @RequestMapping(value = "detect", method = RequestMethod.GET)
    @Permission(authorities = {"Download"})
    public ResponseEntity detect(@RequestParam(name="sv",required = true, defaultValue = "") @Length(min=1, max=32, message = "sv parameter can't empty") String sv
                                ,@RequestParam(name="cv",required = true, defaultValue = "") @Length(min=1, max=32, message = "cv parameter can't empty") String cv) {
        List<ContentVersionEntity> list=contentVersionService.getDetectVersions(sv, cv, UserThreadLocal.get().getLicenceId());
        return ResponseEntity.ok(list);
    }
    @RequestMapping(value = "download", method = RequestMethod.GET)
    @Permission(authorities = {"Download"})
    public void download(@RequestParam(name="sv",required = true, defaultValue = "") @Length(min=1, max=32, message = "sv parameter can't empty") String sv
                         ,@RequestParam(name="cv",required = true, defaultValue = "") @Length(min=1, max=32, message = "cv parameter can't empty") String cv
                         ,HttpServletResponse response) throws Exception {
        ContentPackageEntity contentPackageEntity=contentVersionService.downloadOnePackage(sv, cv);
        if(contentPackageEntity==null) {
            return;
        }
        byte[] data=contentPackageEntity.getData();
        setHeader(response, !StringUtils.isEmpty(contentPackageEntity.getVersion())?contentPackageEntity.getVersion()+".zip":"package.zip",data);
        byte[] buffer = new byte[1024];
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        try {
            OutputStream os = response.getOutputStream();
            int i = bis.read(buffer);
            while (i != -1) {
                os.write(buffer, 0, i);
                i = bis.read(buffer);
            }
            os.flush();
            return ;
        } finally {
            close(bis);
        }
    }
    @RequestMapping(value = "downloadMulti", method = RequestMethod.GET)
    @Permission(authorities = {"Download"})
    public void downloadMulti(@RequestParam(name="sv",required = true, defaultValue = "") @Length(min=1, max=32, message = "sv parameter can't empty") String sv
                       ,@RequestParam(name="cv",required = true) @Size(min=1,message = "cv parameter can't empty") String[] cv
                       ,HttpServletResponse response) throws Exception {
      List<ContentPackageEntity> list=contentVersionService.downloadMultiPackage(sv, cv);
      String fileName ="package.zip";
      if (!CollectionUtils.isEmpty(list)) {
          byte[] dataByteArr = zipFile(list);
          setHeader(response, fileName,dataByteArr);
          response.getOutputStream().write(dataByteArr);
          response.flushBuffer();
      }
    }
    private byte[] zipFile(List<ContentPackageEntity> list) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(baos);
        try {
            for(int i=0;i<list.size();i++) {
                ContentPackageEntity cpe=list.get(i);
                ZipEntry entry = new ZipEntry(cpe.getVersion());
                entry.setSize(cpe.getData().length);
                out.putNextEntry(entry);
                out.write(cpe.getData());
                out.closeEntry();
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return baos.toByteArray();
    }
    private void setHeader(HttpServletResponse response, String fileName,byte[] data) throws NoSuchAlgorithmException {
        response.setContentType("application/octet-stream");
        response.setContentType("application/force-download");
        response.addHeader("Content-Disposition", "attachment;fileName=\"" + fileName + "\"");
        response.addHeader("md5", CommonUtils.getMD5ForBytes(data));
    }
    private void close(ByteArrayInputStream bis) throws IOException {
        if (bis != null) {
                bis.close();
        }
    }
}
