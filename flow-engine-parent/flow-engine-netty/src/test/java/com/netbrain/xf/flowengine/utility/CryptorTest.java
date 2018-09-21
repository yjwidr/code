package com.netbrain.xf.flowengine.utility;

import org.junit.Assert;
import org.junit.Test;

public class CryptorTest {
    @Test
    public void testEncryption() throws Exception {
        String encrypted = "SwMgFTCStbV7W4dIRtw3MA==";
        Assert.assertEquals(encrypted, Cryptor.encrypt("mypassword"));
    }

    @Test
    public void testDecryption() throws Exception {
        String encrypted = "SwMgFTCStbV7W4dIRtw3MA==";
        Assert.assertEquals("mypassword", Cryptor.decrypt(encrypted));
    }
}
