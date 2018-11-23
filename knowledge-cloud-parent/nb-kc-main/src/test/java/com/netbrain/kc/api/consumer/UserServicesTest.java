package com.netbrain.kc.api.consumer;

import java.util.Date;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.netbrain.kc.api.model.datamodel.UserEntity;
import com.netbrain.kc.api.provider.repository.UserRepository;
import com.netbrain.kc.api.service.UserService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServicesTest {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Before
    public void setUp() {
        userRepository.deleteAll();
    }

    @After
    public void after()
    {
        userRepository.deleteAll();
    }
    
    @Test
    public void testSave() throws Exception {
    	UserEntity user =new UserEntity();
    	user.setUserName("testt");
    	user.setRoleId("1");
    	user.setPassword("fdsaf");
    	user.setLoginName("testtttttt");
    	user.setCompany("sss");
    	user.setLicenceId("ttttttttttttt");
    	user.setCreateTime(new Date());
    	user.setUpdateTime(new Date());
    	user.setCreateUserId("12121212");
    	user.setUpdateUserId("bbbbbbbbbbbb");
    	user=userService.save(user);
        Assert.assertNotNull(user.getId());
    }
    
}
