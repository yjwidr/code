package com.xxx.autoupdate.apiserver.dao;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.xxx.autoupdate.apiserver.dao.ContentPackageRepository;
import com.xxx.autoupdate.apiserver.dao.ContentVersionRepository;
import com.xxx.autoupdate.apiserver.model.ContentPackageEntity;
import com.xxx.autoupdate.apiserver.model.ContentVersionEntity;
import com.xxx.autoupdate.apiserver.model.parameter.PackageInfo;
import com.xxx.autoupdate.apiserver.model.parameter.PackageVersionParameter;
import com.xxx.autoupdate.apiserver.services.ContentPackageService;
import com.xxx.autoupdate.apiserver.services.ContentVersionService;
import com.xxx.autoupdate.apiserver.util.CommonUtils;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ContentPackageRepositoryTest {
    @Autowired
    private ContentPackageRepository contentPackageRepository;
    @Autowired
    private ContentVersionRepository contentVersionRepository;
    @Autowired
    private ContentVersionService contentVersionService;

    @Autowired
    private ContentPackageService contentPackageService;
    @Before
    public void setUp() {
//    	contentPackageRepository.deleteAll();
    }

    @After
    public void after()
    {
//    	contentPackageRepository.deleteAll();
    }
    
    @Test
    public void testSave() throws Exception{
   
    }
    @Test
    public void testSaveVersion() throws Exception{
        ContentPackageEntity packageentity = new ContentPackageEntity();
        packageentity.setData(CommonUtils.getBytes("conf/Find_Interface_Weakness0.rbt"));
        packageentity.setDataMd5("123456");
        packageentity.setCreateTime(new Date());
        packageentity.setCreateUserId("10000");
        packageentity.setId("10000");
        packageentity.setVersion("7.1a");
        packageentity = contentPackageRepository.save(packageentity);
        ContentVersionEntity entity = new ContentVersionEntity();
        entity.setId("10000");
        entity.setActivateStatus((short)1);
        entity.setAuthor("cc");
        entity.setCategory((short)1);
        entity.setContentVersion(1002000004);
        entity.setCreateTime(new Date());
        entity.setCreateUserId("10000");
        entity.setDescription("desc");
        entity.setMajorVersion(1);
        entity.setMinorVersion(2);
        entity.setName("ccc");
        entity.setPackageId(packageentity.getId());
        entity.setPublishStatus((short)1);
        entity.setRevisionVersion(4);
        entity.setSoftwareVersion("7.1");
        entity.setType((short)1);
        entity.setUpdateTime(new Date());
        entity.setUpdateUserId("10000");
        entity = contentVersionRepository.save(entity);
        packageentity = new ContentPackageEntity();
        packageentity.setData(CommonUtils.getBytes("conf/Find_Interface_Weakness0.rbt"));
        packageentity.setDataMd5("123456");
        packageentity.setCreateTime(new Date());
        packageentity.setCreateUserId("10000");
        packageentity.setId("20000");
        packageentity.setVersion("7.1a");
        packageentity = contentPackageRepository.save(packageentity);   
        entity.setId("20000");
        entity.setActivateStatus((short)1);
        entity.setAuthor("cc");
        entity.setCategory((short)1);
        entity.setContentVersion(1002000003);
        entity.setCreateTime(new Date());
        entity.setCreateUserId("10000");
        entity.setDescription("desc");
        entity.setMajorVersion(1);
        entity.setMinorVersion(2);
        entity.setName("bbb");
        entity.setPackageId(packageentity.getId());
        entity.setPublishStatus((short)1);
        entity.setRevisionVersion(3);
        entity.setSoftwareVersion("7.1");
        entity.setType((short)1);
        entity.setUpdateTime(new Date());
        entity.setUpdateUserId("10000");
        entity = contentVersionRepository.save(entity);
        assertEquals("10000",entity.getCreateUserId());
    }    
    @Test
    public void testFindContentPackageById() throws Exception {
        Optional<ContentPackageEntity> entity=contentPackageRepository.findById(contentPackageRepository.findAll().get(0).getId());
        if(entity.isPresent()) {
            ContentPackageEntity cpe=entity.get();
            assertEquals("10000",cpe.getCreateUserId());
        }

    }
    
    @Test 
    public void testDownMulti() {
    	List<ContentPackageEntity> packages=contentVersionService.downloadMultiPackage("7.1", new String[] {"1.2.3","1.2.4"});
    	assertEquals(2, packages.size());
    }
    
    //@Test
    public void testPublish() {
    	int res = contentVersionService.publishContentVersion(new String[] {"1111"}, "100000");
    	assertEquals(0,res);
    }
    
    @Test
    public void testGetResourceSummary() throws Exception {
    	Path path = Paths.get("conf//Find_Interface_Weakness0.rbt");
    	byte[] bytes = Files.readAllBytes(path);
    	String value = Base64.encodeBase64String(bytes);
    	PackageVersionParameter info = new PackageVersionParameter();
    	info.setBase64Data(value);
    	PackageInfo pi = contentPackageService.getPackageSummary(info);
    	assertEquals(1, pi.getPackageVersion());
    }

}
