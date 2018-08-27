package com.xxx.autoupdate.apiserver.controller;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.xxx.autoupdate.apiserver.model.parameter.LoginUser;
import com.xxx.autoupdate.apiserver.util.ResponseEntity;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LoginControllerTest {
    @Autowired
    private LoginController loginController;

    @Before
    public void setUp() {

    }

    @After
    public void after() {
    }

    @Test
    public void testSave() throws Exception {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserName("admin");
        loginUser.setPassword("YXBpc2VydmVy");
        ResponseEntity re = loginController.login(loginUser);
        Assert.assertEquals(0, re.getResultCode());
    }

}
