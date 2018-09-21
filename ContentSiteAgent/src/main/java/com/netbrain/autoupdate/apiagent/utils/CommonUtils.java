package com.netbrain.autoupdate.apiagent.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


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
    public static byte[] getBytes(String filePath) throws Exception{  
        byte[] buffer = null;  
        FileInputStream fis=null;
        ByteArrayOutputStream bos =null;
        try {  
            File file = new File(filePath);  
            fis = new FileInputStream(file);  
            bos = new ByteArrayOutputStream(1000);  
            byte[] b = new byte[1000];  
            int n;  
            while ((n = fis.read(b)) != -1) {  
                bos.write(b, 0, n);  
            }  
            buffer = bos.toByteArray();  
        }finally {
            fis.close();  
            bos.close();
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
    public static String getMD5ForBytes(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest mMessageDigest = MessageDigest.getInstance("MD5");
        mMessageDigest.update(bytes);
        return new BigInteger(1, mMessageDigest.digest()).toString(16);
    }
    
    public static byte[] base64decodeTobyte(String value) throws UnsupportedEncodingException {
        final Base64.Decoder decoder = Base64.getDecoder();
        return decoder.decode(value);
    }
    public static String base64decode(String value) throws UnsupportedEncodingException {
        final Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(value), "UTF-8");
    }
    public static String base64encode(byte[] value) throws UnsupportedEncodingException {
        final Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(value);
    }

    public static String getStackMsg(Exception e) throws IOException {
        StringWriter sw =null;
        PrintWriter pw =null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        }finally{
            sw.close();
            pw.close();
        }
    }
    public static  List<String> getFileName(String path) {
        File file = new File(path);
        String[] fileName = file.list();
        return fileName==null?Collections.emptyList():Arrays.asList(fileName);
    }
    public static byte[] zip(byte[] data) {
	    byte[] b = null;
	    try {
	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        ZipOutputStream zos = new ZipOutputStream(bos);    
	        ByteArrayInputStream bis = new ByteArrayInputStream(data);
	        ZipInputStream zis = new ZipInputStream(bis);
	        ZipEntry ze  = null;
	        while ((ze=zis.getNextEntry())!= null) {
	        byte[] buf = new byte[1024];
	        int num = -1;
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        while ((num = zis.read(buf, 0, buf.length)) != -1) {
	            baos.write(buf, 0, num);
	        }
	        ZipEntry entry = new ZipEntry(ze.getName());
	        entry.setSize(ze.getSize());
	        zos.putNextEntry(entry);
	        zos.write(baos.toByteArray());
	        zos.closeEntry();
	        baos.flush();
	        baos.close();
	        }
	        b = bos.toByteArray();
	        System.out.println(b.length);
	        zos.close();
	        bos.close();
	        zis.close();
	        bis.close();
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	    return b;
    }    
}
