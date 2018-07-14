package com.netbrain.autoupdate.apiserver.dao;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.netbrain.autoupdate.apiserver.model.FileEntity;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileRepositoryTest {
    @Autowired
    private FileRepository fileRepository;
    @Before
    public void setUp() {
        fileRepository.deleteAll();
    }

    @After
    public void after()
    {
        fileRepository.deleteAll();
    }
    
    @Test
    public void testSave() throws Exception {
        fileRepository.save(new FileEntity());
        Assert.assertEquals(1, fileRepository.findAll().size());
    }
    
    @Test
    public void test$() {
    	String a= "2.3.3$";
    	String b = a.replace("$", "");
    	Assert.assertEquals(b, "2.3.3");
    }

}
