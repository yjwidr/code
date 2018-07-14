package com.netbrain.autoupdate.apiserver.controller;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.netbrain.autoupdate.apiserver.dao.FileRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileControllerTest {
    @Autowired
    private FileController fileController;
    @Autowired
    private FileRepository fileRepository;

    @Before
    public void setUp() {
         fileRepository.deleteAll();
    }

    @After
    public void after() {
//         fileRepository.deleteAll();
    }

    @Test
    public void testSave() throws Exception {
        fileController.save();
        Assert.assertEquals(1, fileRepository.findAll().size());
    }

}
