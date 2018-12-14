package com.netbrain.kc.framework.encrpt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jasypt.encryption.StringEncryptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EncryptorTest {
	private static Logger logger = LogManager.getLogger(EncryptorTest.class.getName());
    @Autowired
    private StringEncryptor stringEncryptor;
    @Test
    public void encrypt() {
        String result = stringEncryptor.encrypt("postgres");
        logger.debug("Encrypt result:{}",result);
    }
    @Test
    public void decrypt() {
    	String result = stringEncryptor.encrypt("postgres");
    	result = stringEncryptor.decrypt(result);
    	logger.debug("Decrypt result:{}",result);
    }
}
