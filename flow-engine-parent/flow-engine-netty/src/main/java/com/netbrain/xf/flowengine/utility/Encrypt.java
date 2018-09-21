package com.netbrain.xf.flowengine.utility;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

  
public class Encrypt {
    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM_ECB = "AES/ECB/PKCS5Padding"; 
    private static Logger logger = LogManager.getLogger(Encrypt.class.getName());
    public static String encrypt(String content) {
        if(StringUtils.isEmpty(content)) {
            logger.error("password cannot be empty.");
            return null;
        }
        try {
            SecretKey secretKey = KeyGenerator.getInstance(KEY_ALGORITHM).generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKey skeySpec = new SecretKeySpec(enCodeFormat, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_ECB);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(content.getBytes("UTF-8"));
            return new String(Base64.getEncoder().encode(encrypted))+"_"+new String(Base64.getEncoder().encode(enCodeFormat));
        }catch(Exception e) {
            e.printStackTrace();
            logger.error("encrypt: {}",content);
            return null;
        }
    }

    public static String decrypt(String encrypt){
        if(StringUtils.isEmpty(encrypt)) {
            logger.error("encrypt cannot be empty.");
            return null; 
        }
        String[] array=StringUtils.split(encrypt, "_");
        if(array.length!=2) {
            logger.error("encrypt incorrect: {}",encrypt);
            return null;
        }
        try {
            SecretKey skeySpec = new SecretKeySpec(Base64.getDecoder().decode(array[1]), KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_ECB);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] encrypted1 = Base64.getDecoder().decode(array[0]);
            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original,"UTF-8");
            return originalString;
        }catch(Exception e) {
            e.printStackTrace();
            logger.error("encrypt: {}",encrypt);
            return null;
        }
    }
    public static void main(String[] args) {
        System.out.println(encrypt("mongo"));
    }
}      
