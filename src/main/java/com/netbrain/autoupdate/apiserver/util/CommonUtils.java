package com.netbrain.autoupdate.apiserver.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class CommonUtils {
    public static <T> List<T> compare(T[] methodAuthorities, T[] userAuthorities) {
        List<T> listUser = Arrays.asList(userAuthorities);
        List<T> listDifferent = new ArrayList<T>();
        for (T t : methodAuthorities) {
            if (!listUser.contains(t)) {
                listDifferent.add(t);
            }
        }
        return listDifferent;
    }
    
    public static <T> T toObject(Optional<T> optionalObject) {
    	if(optionalObject.isPresent()) {
    		return optionalObject.get();
    	}else {
    		return null;
    	}
    }

    public static <T> boolean hasAuthorities(T[] methodAuthorities, T[] userAuthorities) {
        List<T> listUser = Arrays.asList(userAuthorities);
        List<T> listDifferent = new ArrayList<T>();
        for (T t : methodAuthorities) {
            if (!listUser.contains(t)) {
                listDifferent.add(t);
            }
        }
        return listDifferent.size() == 0 ? true : false;
    }

    public static SimpleFilterProvider builderJsonFilter(String[] fileds, String className) {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        String[] filedNames = fileds;
        Set<String> filter = new HashSet<String>();
        if (filedNames != null && filedNames.length > 0) {
            for (int i = 0; i < filedNames.length; i++) {
                String filedName = filedNames[i];
                if (!StringUtils.isEmpty(filedName)) {
                    filter.add(filedName);
                }
            }
        }
        filterProvider.addFilter(className, SimpleBeanPropertyFilter.filterOutAllExcept(filter));
        return filterProvider;
    }

    public static Object ByteToObject(byte[] bytes) {
        Object obj = null;
        try {
            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
            ObjectInputStream oi = new ObjectInputStream(bi);

            obj = oi.readObject();
            bi.close();
            oi.close();
        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return obj;
    }

    public static byte[] ObjectToByte(java.lang.Object obj) {
        byte[] bytes = null;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);

            bytes = bo.toByteArray();

            bo.close();
            oo.close();
        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return bytes;
    }
    public static String base64decode(String value) throws UnsupportedEncodingException {
        final Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(value), "UTF-8");
    }
 
    public static byte[] getBytes(String filePath){  
        byte[] buffer = null;  
        try {  
            File file = new File(filePath);  
            FileInputStream fis = new FileInputStream(file);  
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);  
            byte[] b = new byte[1000];  
            int n;  
            while ((n = fis.read(b)) != -1) {  
                bos.write(b, 0, n);  
            }  
            fis.close();  
            bos.close();  
            buffer = bos.toByteArray();  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
        return buffer;  
    }

    public static void bytesToFile(byte[] bfile, String filePath) throws IOException {  
        BufferedOutputStream bos = null;  
        FileOutputStream fos = null;  
        File file = null;  
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {
                dir.mkdirs();
            }
            file = new File(filePath);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } finally {
            if (bos != null) {
                bos.close();
            }
            if (fos != null) {
                fos.close();
            }
        }  
    }
    public static String getMD5ForFile(File file) throws NoSuchAlgorithmException, IOException {
        InputStream fis = new FileInputStream(file);
        MessageDigest mMessageDigest = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[1024];
        int length = -1;
        while ((length = fis.read(buffer, 0, 1024)) > 0) {
            mMessageDigest.update(buffer, 0, length);
        }
        fis.close();
        return new BigInteger(1, mMessageDigest.digest()).toString(16);
    }
    public boolean isSmaeFile(String filePath1, String filePath2) throws NoSuchAlgorithmException, IOException {
        boolean result = true;
        File file1 = new File(filePath1);
        File file2 = new File (filePath2);
        if (file1.length() != file2.length()) {
            result = false;
        } else {
            String file1MD5 = getMD5ForFile(file1);
            String file2MD5 = getMD5ForFile(file2);
            if (file1MD5 != null && !file1MD5.equals(file2MD5)) {
                result = false;
            }
        }
        return result;
    }

    public static String getMD5ForBytes(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest mMessageDigest = MessageDigest.getInstance("MD5");
        mMessageDigest.update(bytes);
        return new BigInteger(1, mMessageDigest.digest()).toString(16);
    }
}
