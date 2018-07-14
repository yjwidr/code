package com.netbrain.autoupdate.apiserver.dao;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.netbrain.autoupdate.apiserver.model.UserEntity;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;
    @Before
    public void setUp() {
//        userRepository.deleteAll();
    }

    @After
    public void after()
    {
//        userRepository.deleteAll();
    }
    
    @Test
    public void testSave() throws Exception {
        userRepository.save(new UserEntity());
        Assert.assertEquals(1, userRepository.findAll().size());
    }

}
