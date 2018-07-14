package com.netbrain.autoupdate.apiserver.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.netbrain.autoupdate.apiserver.model.ContentVersionEntity;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ContentVersionRepositoryTest {
    @Autowired
    private ContentVersionRepository contentVersionRepository;
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
    	entity.setMinorVersion(1);
    	entity.setName("ccc");
    	entity.setPackageId("10000");
    	entity.setPublishStatus((short)1);
    	entity.setRevisionVersion(4);
    	entity.setSoftwareVersion("7.1");
    	entity.setType((short)1);
    	entity.setUpdateTime(new Date());
    	entity.setUpdateUserId("10000");
        entity = contentVersionRepository.save(entity);
        entity.setId("20000");
        entity.setActivateStatus((short)1);
        entity.setAuthor("cc");
        entity.setCategory((short)1);
        entity.setContentVersion(1002000003);
        entity.setCreateTime(new Date());
        entity.setCreateUserId("10000");
        entity.setDescription("desc");
        entity.setMajorVersion(1);
        entity.setMinorVersion(1);
        entity.setName("bbb");
        entity.setPackageId("10000");
        entity.setPublishStatus((short)1);
        entity.setRevisionVersion(4);
        entity.setSoftwareVersion("7.1");
        entity.setType((short)1);
        entity.setUpdateTime(new Date());
        entity.setUpdateUserId("10000");
        entity = contentVersionRepository.save(entity);
    	assertEquals(entity.getCreateUserId(),"10000");
    }
    
    @Test
    public void testFindContentPackageById() throws Exception {

    }

}
