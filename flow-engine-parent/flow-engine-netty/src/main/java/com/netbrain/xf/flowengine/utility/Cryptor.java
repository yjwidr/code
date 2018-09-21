package com.netbrain.xf.flowengine.utility;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

/**
 * An encryptor/decryptor following the same algorithm defined in NSUTil/Des.cpp
 * Please note that this is not a standard PKCS#7 padding.
 */
public class Cryptor {
    private static final String KEY_ALGORITHM = "DESede";
    private static final String CIPHER_ALGORITHM_ECB = "DESede/ECB/NoPadding";
    private static Logger logger = LogManager.getLogger(Cryptor.class.getSimpleName());

    private static final byte[] keyBytes = { (byte) -104, (byte) -70, (byte) -85, (byte) -92,
            (byte) 107, (byte) 25, (byte) 44, (byte) -119, (byte) 98, (byte) -60, (byte) -65, (byte) 49, (byte) -63,
            (byte) -85, (byte) -17, (byte) 100, (byte) -77, (byte) 81, (byte) -42, (byte) 44, (byte) -108, (byte) 91,
            (byte) 64, (byte) 87 };

    public static String encrypt(String content) throws InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException {
        final SecretKey key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_ECB);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        int iLen = content.length();

        int iPlus = (8 - iLen % 8);
        byte[] szInput = content.getBytes();
        byte[] szInputPadding = new byte[iLen + iPlus];
        for(int i = 0; i < iLen + iPlus; i++)
        {
            if(i < iLen)
                szInputPadding[i] = szInput[i];
            else
                szInputPadding[i] = 7;
        }

        final byte[] cipherText = cipher.doFinal(szInputPadding);
        String base64Encoded = new String(Base64.getEncoder().encode(cipherText));
        return base64Encoded.replace('+', '@').replace('/', '$');
    }

    public static String decrypt(String encryptedBase64Text) throws
            InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException,
            BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        final SecretKey key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        final Cipher decipher = Cipher.getInstance(CIPHER_ALGORITHM_ECB);
        decipher.init(Cipher.DECRYPT_MODE, key);

        final byte[] message = Base64.getDecoder().decode(encryptedBase64Text.replace('@', '+')
                .replace('$', '/'));
        final byte[] plainText = decipher.doFinal(message);

        int firstSeven = 0;
        for(int i = 0; i < plainText.length; i++)
        {
            if(plainText[i] == 7) {
                firstSeven = i;
                break;
            }
        }

        byte[] noPaddingText = new byte[firstSeven];
        for (int i = 0; i < firstSeven; i++) {
            noPaddingText[i] = plainText[i];
        }
        return new String(noPaddingText, "UTF-8");
    }
    public static void main(String[] args) {
        try {
            System.out.println(decrypt("B$AQkTz4lic="));
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}      
