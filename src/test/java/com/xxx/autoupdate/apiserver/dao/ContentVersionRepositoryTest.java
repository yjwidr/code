package com.xxx.autoupdate.apiserver.dao;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import com.xxx.autoupdate.apiserver.dao.ContentPackageRepository;
import com.xxx.autoupdate.apiserver.dao.ContentVersionRepository;
import com.xxx.autoupdate.apiserver.model.ContentPackageEntity;
import com.xxx.autoupdate.apiserver.model.ContentVersionEntity;
import com.xxx.autoupdate.apiserver.model.parameter.ContentVersion;
import com.xxx.autoupdate.apiserver.model.parameter.UploadOneContent;
import com.xxx.autoupdate.apiserver.services.ContentVersionService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ContentVersionRepositoryTest {
    @Autowired
    private ContentVersionRepository contentVersionRepository;
    @Autowired
    private ContentPackageRepository contentPackageRepository;
    @Autowired
    private ContentVersionService contentVersionService;
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
		ContentPackageEntity cp = new ContentPackageEntity();
		cp.setCreateTime(new Date());
		cp.setCreateUserId("10000");
		cp.setData(new byte[] {1,2,3,4});
		cp.setResourceCount(10);
		cp = contentPackageRepository.save(cp);
		ContentVersionEntity entity = new ContentVersionEntity();
		entity.setId("10000");
		entity.setActivateStatus((short)1);
		entity.setAuthor("cc");
		entity.setCategory((short)1);
		entity.setContentVersion(1002000000);
		entity.setCreateTime(new Date());
		entity.setCreateUserId("10000");
		entity.setDescription("desc");
		entity.setMajorVersion(1);
		entity.setMinorVersion(2);
		entity.setName("bbb");
		entity.setPackageId(cp.getId());
		entity.setPublishStatus((short)1);
		entity.setRevisionVersion(4);
		entity.setSoftwareVersion("7.1");
		entity.setType((short)1);
		entity.setUpdateTime(new Date());
		entity.setUpdateUserId("10000");
		entity = contentVersionRepository.save(entity);

        List<ContentVersionEntity> list =contentVersionService.getAllContentVersionList();
        assertEquals(1, list.size());
    }
    @Test
    public void testUpload() throws IOException {
    	UploadOneContent content = new UploadOneContent();
    	File file = new File("conf//Find_Interface_Weakness0.rbt");
    	FileInputStream fis = new FileInputStream(file);
    	MultipartFile multi = new MockMultipartFile("new.rbt", fis);
    	content.setFile(multi);
    	content.setContentName("name");
    	content.setContentPackageType((short)1);
    	ContentVersion  cv = new ContentVersion();
    	cv.setMajor(1);
    	cv.setMinor(2);
    	cv.setRevision(0);
    	content.setContentVersion(cv);
    	content.setSupportSoftwareVersion("7.5");
    	Date d1= new Date();
    	contentVersionService.uploadContentVersion2(content, "userid_admin");
    	long sec = (new Date().getTime() - d1.getTime())/1000;
    	cv.setMinor(3);
    	content.setContentVersion(cv);
    	content.setContentName("name2");
    	Date d2= new Date();
    	contentVersionService.uploadContentVersion2(content, "userid_admin");
    	long sec2 = (new Date().getTime() - d2.getTime())/1000;
    	System.out.println(sec+"_"+sec2);
    }
    
    @Test
    public void testGetLastVersion() {
    	int[] a = contentVersionService.getLastContentVersionOfSpacialPkg("7.1", 1, 2);
    	assertEquals(a[0],1);
    }
    
    @Test
    public void testDetect() {
    	String software = "7.1.10";
    	String cv = "0.0.0";
    	List<ContentVersionEntity> es =contentVersionService.getDetectVersions(software, cv,"");
    	assertEquals(es.size()>0, true);
    	
    }
    

}
