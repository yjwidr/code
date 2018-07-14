package com.netbrain.autoupdate.apiserver.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MD5UtilTest {
    @Test
    public void testMd5Salt() throws Exception {
        String plaintext = "apiserver";
        System.out.println("plain：" + plaintext);
        System.out.println("after md5：" + MD5Util.MD5(plaintext));
        String salt = MD5Util.GeneratingSalt();
        System.out.println("salt：" + salt);
        String ciphertext = MD5Util.generate(plaintext);
        String ciphertext2 = MD5Util.generate(plaintext);
        System.out.println("md5 salt：" + ciphertext +","+ciphertext2);
        System.out.println("verify:" + MD5Util.verify(plaintext, ciphertext));

        String[] tempSalt = { "c4d980d6905a646d27c0c437b1f046d4207aa2396df6af86",
                "66db82d9da2e35c95416471a147d12e46925d38e1185c043",
                "61a718e4c15d914504a41d95230087a51816632183732b5a" };

        for (String temp : tempSalt) {
            System.out.println("verify:" + MD5Util.verify(plaintext, temp));

        }
    }
}
