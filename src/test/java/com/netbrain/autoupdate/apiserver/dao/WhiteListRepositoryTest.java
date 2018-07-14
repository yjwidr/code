package com.netbrain.autoupdate.apiserver.dao;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WhiteListRepositoryTest {
    @Autowired
    private WhiteListRepository whiteListRepository;
    @Before
    public void setUp() {
    	whiteListRepository.deleteAll();
    }

    @After
    public void after()
    {
    	whiteListRepository.deleteAll();
    }
    
    @Test
    public void test() {
    	
    }
    
    

}
