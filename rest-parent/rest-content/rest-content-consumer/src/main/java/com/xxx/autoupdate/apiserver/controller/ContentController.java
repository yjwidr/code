package com.xxx.autoupdate.apiserver.controller;

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
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.xxx.autoupdate.apiserver.annotation.Permission;
import com.xxx.autoupdate.apiserver.exception.BusinessException;
import com.xxx.autoupdate.apiserver.exception.ErrorCodes;
import com.xxx.autoupdate.apiserver.model.ContentPackageEntity;
import com.xxx.autoupdate.apiserver.model.ContentVersionEntity;
import com.xxx.autoupdate.apiserver.model.SoftwareVersionEntity;
import com.xxx.autoupdate.apiserver.model.constant.Constants;
import com.xxx.autoupdate.apiserver.model.constant.ContentPackageType;
import com.xxx.autoupdate.apiserver.model.parameter.ContentVersionIds;
import com.xxx.autoupdate.apiserver.model.parameter.PackageVersionParameter;
import com.xxx.autoupdate.apiserver.model.parameter.UpdateContentParameter;
import com.xxx.autoupdate.apiserver.model.parameter.UploadOneContent;
import com.xxx.autoupdate.apiserver.services.ContentPackageService;
import com.xxx.autoupdate.apiserver.services.ContentVersionService;
import com.xxx.autoupdate.apiserver.services.SoftwareVersionService;
import com.xxx.autoupdate.apiserver.util.CommonUtils;
import com.xxx.autoupdate.apiserver.util.ContentVersionConvert;
import com.xxx.autoupdate.apiserver.util.ResponseEntity;
import com.xxx.autoupdate.apiserver.util.UserThreadLocal;

@RestController
@RequestMapping(value = Constants.CONTENT)
@Validated
public class ContentController {
    private static Logger logger = LogManager.getLogger(ContentController.class.getName());
    
    @Reference
    private SoftwareVersionService softwareVersionService;
    @Reference
    private ContentVersionService contentVersionService;
    @Reference
    private ContentPackageService contentPackageService;
    
    @RequestMapping(value = Constants.LIST, method = RequestMethod.GET)
    public MappingJacksonValue list(){
        List<ContentVersionEntity> list=contentVersionService.getAllContentVersionList();
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(ContentVersionEntity.class.getName(),SimpleBeanPropertyFilter.filterOutAllExcept(
                new String[] {Constants.ID,Constants.NAME,Constants.TYPE,Constants.SOV,Constants.CPV,Constants.PS,Constants.AS,Constants.AU,Constants.DESC,Constants.RC,Constants.CT,Constants.CUN,Constants.UT}));
        MappingJacksonValue jacksonValue =new MappingJacksonValue(ResponseEntity.ok(list)) ;
        jacksonValue.setFilters(filterProvider);
        return jacksonValue;
    }
    @RequestMapping(value = Constants.SWVL, method = RequestMethod.GET)
    public MappingJacksonValue getSoftwareVersionList(){
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(SoftwareVersionEntity.class.getName(),SimpleBeanPropertyFilter.filterOutAllExcept(new String[] {Constants.VER,Constants.NAME,Constants.DESC,Constants.CT,Constants.UT}));
        MappingJacksonValue jacksonValue =new MappingJacksonValue(ResponseEntity.ok(softwareVersionService.getSoftwareVersionList())) ;
        jacksonValue.setFilters(filterProvider);
        return jacksonValue;
    }
    @RequestMapping(value = Constants.IVT, method = RequestMethod.POST)
    @Permission(authorities={Constants.UPLOAD})
    public ResponseEntity inventory(@RequestBody @Valid PackageVersionParameter packageVersionParameter){
        return ResponseEntity.ok(contentPackageService.getPackageSummary(packageVersionParameter));
    }
    @RequestMapping(value = Constants.UPDATE, method = RequestMethod.POST)
    @Permission(authorities={Constants.UPLOAD})
    public ResponseEntity update(@RequestBody @Valid UpdateContentParameter ucp){
        return ResponseEntity.ok(contentVersionService.updateContentVersionNameAndDesc(ucp.getId(), ucp.getName(),ucp.getDescription(), UserThreadLocal.get().getId()));
    }
    @RequestMapping(value = Constants.LMV, method = RequestMethod.GET)
    public ResponseEntity lastMinorVersion(@RequestParam(name=Constants.SVP,required = true, defaultValue = Constants.EMP) @Length(min=1, max=32, message = Constants.SV) String sv){
        int[] cv=contentVersionService.getMaxContentMinorVersion(sv);
        if(cv.length==Constants.CVLEN) {
            Map<String,Integer> map = new HashMap<>();
            map.put(Constants.MAJOR, Integer.valueOf(cv[0])); 
            map.put(Constants.MINOR, Integer.valueOf(cv[1]));
            map.put(Constants.REVISION, Integer.valueOf(cv[2]));
            return ResponseEntity.ok(map);
        }else {
            return ResponseEntity.ok();
        }
    }
    @RequestMapping(value = Constants.LRV, method = RequestMethod.GET)
    public ResponseEntity lastRevisionVersion(@RequestParam(name=Constants.SVP,required = true, defaultValue = Constants.EMP) @Length(min=1, max=32, message = Constants.SV) String sv
                                              ,@RequestParam(name=Constants.CVMP,required = true, defaultValue = Constants.EMP) @Length(min=1, max=32, message = Constants.CVM) @Pattern(regexp=Constants.CVMREG,message=Constants.CVMPATTERN) String cvm) {
        String[] cvms=cvm.split(Constants.XGD);
        int[] cv=contentVersionService.getLastContentVersionOfSpacialPkg(sv,Integer.parseInt(cvms[0]),Integer.parseInt(cvms[1]));
        if(cv.length==Constants.CVLEN) {
            Map<String,Integer> map = new HashMap<>();
            map.put(Constants.MAJOR, Integer.valueOf(cv[0])); 
            map.put(Constants.MINOR, Integer.valueOf(cv[1]));
            map.put(Constants.REVISION, Integer.valueOf(cv[2]));
            return ResponseEntity.ok(map);
        }else {
            return ResponseEntity.ok();
        }
    }
    @RequestMapping(value = Constants.ADD, method = RequestMethod.POST)
    @Permission(authorities={Constants.UPLOAD})
    public ResponseEntity add(@Valid UploadOneContent uploadContent){
        UploadOneContent uoc=new UploadOneContent();
        uoc=JSON.parseObject(uploadContent.getInfo(), UploadOneContent.class);
        uoc.setFile(uploadContent.getFile());
        String softwareVersion = uoc.getSupportSoftwareVersion();
        int major = uoc.getContentVersion().getMajor();
        int minor = uoc.getContentVersion().getMinor();
        int revision = uoc.getContentVersion().getRevision();
        if(StringUtils.isEmpty(uoc.getContentName())){
            throw new BusinessException(ErrorCodes.ERROR_CONTENTNAME_EMPTY); 
        }else if(uoc.getContentPackageType()>(short)ContentPackageType.PatchPackage || uoc.getContentPackageType()<(short)ContentPackageType.WholePackage){
            throw new BusinessException(ErrorCodes.ERROR_CONTENT_PACKAGETYPE); 
        }else if(StringUtils.isEmpty(softwareVersion)){
            throw new BusinessException(ErrorCodes.ERROR_SUPPORT_SOFTWARE_VERSION_EMPTY); 
        }else if(ObjectUtils.isEmpty(uoc.getContentVersion())){
            throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_EMPTY); 
        }else if(major<=0 || major >= ContentVersionConvert.THOUSAND){
            throw new BusinessException(ErrorCodes.ERROR_RANGE_OF_CONTENT_VERSION_INVALID);
        }else if(minor<=0 || minor >= ContentVersionConvert.THOUSAND){
            throw new BusinessException(ErrorCodes.ERROR_RANGE_OF_CONTENT_VERSION_INVALID);
        }else if(revision<0 || revision >= ContentVersionConvert.MILLION){
            throw new BusinessException(ErrorCodes.ERROR_RANGE_OF_CONTENT_VERSION_INVALID);
        }
        if(uoc.getContentPackageType() == ContentPackageType.WholePackage) {
        	if(revision != 0) {
                throw new BusinessException(ErrorCodes.ERROR_WHOLE_PACKAGE_REVISION_MUSTBE_0);
        	}
        	int[] lastVersions = contentVersionService.getMaxContentMinorVersion(softwareVersion);
        	long lastLong = ContentVersionConvert.convertContentVersion(lastVersions[0],lastVersions[1],0);
        	long curLong = ContentVersionConvert.convertContentVersion(major, minor, revision);
        	if(lastLong >= curLong) {
                throw new BusinessException(ErrorCodes.ERROR_CONTENT_VERSION_MINOR_INVALID.getCode(),String.format(ErrorCodes.ERROR_CONTENT_VERSION_MINOR_INVALID.getMessage(),major,minor,softwareVersion,lastVersions[1],minor));
        	}
        }else {
        	long minorVersion = ContentVersionConvert.convertContentVersion(major, minor, 0);
        	boolean exist = contentVersionService.existMinorContentVersion(softwareVersion, minorVersion);
        	if(exist != true) {
                throw new BusinessException(ErrorCodes.ERROR_CANT_FIND_WHOLE_CONTENTVERSION.getCode(),String.format(ErrorCodes.ERROR_CANT_FIND_WHOLE_CONTENTVERSION.getMessage(),major,minor,softwareVersion));
        	}
        	int[] lastVersions = contentVersionService.getLastContentVersionOfSpacialPkg(softwareVersion, major, minor);
        	if(lastVersions[2] >= revision) {
                throw new BusinessException(ErrorCodes.ERROR_REVISION_LESSTHAN.getCode(),String.format(ErrorCodes.ERROR_REVISION_LESSTHAN.getMessage(),major,minor,softwareVersion,lastVersions[2],revision));
        	}
        }
        long startTime=System.currentTimeMillis();
        contentVersionService.uploadContentVersion2(uoc,UserThreadLocal.get().getId());
        logger.debug("add elapsed time={}ms",System.currentTimeMillis()-startTime);
        return ResponseEntity.ok();
    }
    @RequestMapping(value = Constants.PUB, method = RequestMethod.POST)
    @Permission(authorities={Constants.PUBLISH})
    public ResponseEntity publish(@RequestBody @Valid ContentVersionIds contentVersionIds ){
        int count=contentVersionService.publishContentVersion(contentVersionIds.getContentVersionIds(), UserThreadLocal.get().getId());
        if(count == 0) {
            throw new BusinessException(ErrorCodes.ERROR_PUBLISH);
        }
        return ResponseEntity.ok();
    }
    @RequestMapping(value = Constants.DIS, method = RequestMethod.POST)
    @Permission(authorities={Constants.DISABLE})
    public ResponseEntity disable(@RequestBody @Valid ContentVersionIds contentVersionIds ){
        int count=contentVersionService.disableContentVersion(contentVersionIds.getContentVersionIds(), UserThreadLocal.get().getId());
        if(count == 0) {
        	throw new BusinessException(ErrorCodes.ERROR_DISABLE);
        }
        return ResponseEntity.ok();
    }
    @RequestMapping(value = Constants.TEC, method = RequestMethod.GET)
    @Permission(authorities = {Constants.DOWNLOAD})
    public ResponseEntity detect(@RequestParam(name=Constants.SVP,required = true, defaultValue = Constants.EMP) @Length(min=1, max=32, message = Constants.SV) String sv
                                ,@RequestParam(name=Constants.CVP,required = true, defaultValue = Constants.EMP) @Length(min=1, max=32, message = Constants.CV) String cv) {
        List<ContentVersionEntity> list=contentVersionService.getDetectVersions(sv, cv, UserThreadLocal.get().getLicenceId());
        return ResponseEntity.ok(list);
    }
    @RequestMapping(value = Constants.DWD, method = RequestMethod.GET)
    @Permission(authorities = {Constants.DOWNLOAD})
    public void download(@RequestParam(name=Constants.SVP,required = true, defaultValue = Constants.EMP) @Length(min=1, max=32, message = Constants.SV) String sv
                         ,@RequestParam(name=Constants.CVP,required = true, defaultValue = Constants.EMP) @Length(min=1, max=32, message = Constants.CV) String cv
                         ,HttpServletResponse response) throws Exception {
        ContentPackageEntity contentPackageEntity=contentVersionService.downloadOnePackage(sv, cv);
        if(contentPackageEntity==null) {
            return;
        }
        byte[] data=contentPackageEntity.getData();
        setHeader(response, !StringUtils.isEmpty(contentPackageEntity.getVersion())?contentPackageEntity.getVersion()+Constants.CPKG:Constants.DLCPKG,data);
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
    @RequestMapping(value = Constants.DMT, method = RequestMethod.GET)
    @Permission(authorities = {Constants.DOWNLOAD})
    public void downloadMulti(@RequestParam(name=Constants.SVP,required = true, defaultValue =Constants.EMP) @Length(min=1, max=32, message = Constants.SV) String sv
                       ,@RequestParam(name=Constants.CVRP,required = true) @Size(min=1,message = Constants.CVR) String[] cvr
                       ,HttpServletResponse response) throws Exception {
      long startTime=System.currentTimeMillis();  
      List<ContentPackageEntity> list=contentVersionService.downloadMultiPackage(sv, cvr);
      logger.debug("downloadMultiPackage elapsed time={}ms",System.currentTimeMillis()-startTime);
      String fileName =Constants.PKGZIP;
      if (!CollectionUtils.isEmpty(list)) {
          byte[] dataByteArr = zipFile(list);
          setHeader(response, fileName,dataByteArr);
          response.getOutputStream().write(dataByteArr);
          response.flushBuffer();
      }
      logger.debug("downloadMulti elapsed time={}ms",System.currentTimeMillis()-startTime);
    }
    private byte[] zipFile(List<ContentPackageEntity> list) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(baos);
        try {
            for(int i=0;i<list.size();i++) {
                ContentPackageEntity cpe=list.get(i);
                ZipEntry entry = new ZipEntry(cpe.getVersion()+Constants.CPKG);
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
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentType("application/force-download");
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;fileName=\"" + fileName + "\"");
        response.addHeader(Constants.MD5, CommonUtils.getMD5ForBytes(data));
        response.addHeader(Constants.FN, fileName);
        response.setContentLength(data.length);
        
    }
    private void close(ByteArrayInputStream bis) throws IOException {
        if (bis != null) {
                bis.close();
        }
    }
}
