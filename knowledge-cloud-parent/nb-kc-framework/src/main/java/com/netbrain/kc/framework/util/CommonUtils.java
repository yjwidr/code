package com.netbrain.kc.framework.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.springframework.http.converter.json.MappingJacksonValue;

import com.alibaba.fastjson.JSON;
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

    public static MappingJacksonValue builderJsonFilter(String className,boolean serializeAll,Object object,String... propertyArray) {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        if(serializeAll) {
        	filterProvider.addFilter(className, SimpleBeanPropertyFilter.serializeAll());
        }else{
        	filterProvider.addFilter(className, SimpleBeanPropertyFilter.filterOutAllExcept(propertyArray));
        }
        MappingJacksonValue jacksonValue =new MappingJacksonValue(ResponseEntity.ok(object)) ;
        jacksonValue.setFilters(filterProvider);
        return jacksonValue;
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

	/**
	 * 
	 * @param object
	 * @return
	 */
    public static byte[] convertObjectToJsonBytes(Object object) {
    	return JSON.toJSONBytes(object);
    }
    
    /**
     * 先转换为json格式字符串，在转换为相应的object
     * @param bytes
     * @param clazz
     * @return
     */
    public static <T> T convertJsonBytesToObject(byte[] bytes, Class<T> clazz) {
    	if(bytes.length == 0) {
    		return null;
    	}
    	T t = JSON.parseObject(bytes, clazz);
    	return t;
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
    
    public static byte[] convertIntToByte(int i) {
    	byte[] result = new byte[4];

    	result[3] = (byte) ((i & 0xFF000000) >> 24);
    	result[2] = (byte) ((i & 0x00FF0000) >> 16);
    	result[1] = (byte) ((i & 0x0000FF00) >> 8);
    	result[0] = (byte) ((i & 0x000000FF) >> 0);

    	return result;
    }
    
    public static int convertByteToInt(byte[] b)
	{           
    	int MASK = 0xFF;
        int result = 0;   
            result = b[0] & MASK;
            result = result + ((b[1] & MASK) << 8);
            result = result + ((b[2] & MASK) << 16);
            result = result + ((b[3] & MASK) << 24);            
        return result;      
	}
	
    public static byte[] getFromBase64(String base64Str) {
		byte[] asBytes = Base64.getDecoder().decode(base64Str);
		return asBytes;
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

    static final int BUFFER = 8192;
    public static void zip(String srcPath , String dstPath) throws IOException{
        File srcFile = new File(srcPath);
        File dstFile = new File(dstPath);
        if (!srcFile.exists()) {
            throw new FileNotFoundException(srcPath + " not exist!");
        }
	    if (dstFile != null && dstFile.exists())
	    {
	    	dstFile.delete();
	    }

        FileOutputStream out = null;
        ZipOutputStream zipOut = null;
        try {
            out = new FileOutputStream(dstFile);
            CheckedOutputStream cos = new CheckedOutputStream(out,new CRC32());
            zipOut = new ZipOutputStream(cos);
            String baseDir = "";
            //compress(srcFile, zipOut, baseDir);
            
            File[] files = srcFile.listFiles();
            for (int i = 0; i < files.length; i++) {
                compress(files[i], zipOut, baseDir);
            }
        }
        finally {
            if(null != zipOut){
                zipOut.close();
                out = null;
            }

            if(null != out){
                out.close();
            }
        }
    }
    

    private static void compress(File file, ZipOutputStream zipOut, String baseDir) throws IOException{
        if (file.isDirectory()) {
            compressDirectory(file, zipOut, baseDir);
        } else {
            compressFile(file, zipOut, baseDir);
        }
    }

    /** 压缩一个目录 */
    private static void compressDirectory(File dir, ZipOutputStream zipOut, String baseDir) throws IOException{
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            compress(files[i], zipOut, baseDir + dir.getName() + "/");
        }
    }

    /** 压缩一个文件 */
    private static void compressFile(File file, ZipOutputStream zipOut, String baseDir)  throws IOException{
        if (!file.exists()){
            return;
        }

        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            ZipEntry entry = new ZipEntry(baseDir + file.getName());
            zipOut.putNextEntry(entry);
            int count;
            byte data[] = new byte[BUFFER];
            while ((count = bis.read(data, 0, BUFFER)) != -1) {
                zipOut.write(data, 0, count);
            }

        }finally {
            if(null != bis){
                bis.close();
            }
        }
    }
    
    /**
     * Unzips a file, placing its contents in the given output location.
     *
     * @param zipFilePath
     *            input zip file
     * @param outputLocation
     *            zip file output folder
     * @throws IOException
     *             if there was an error reading the zip file or writing the unzipped data
     */
    public static void unzip(final String zipFilePath, final String outputLocation) throws IOException {
    	// Open the zip file
    	try (final ZipFile zipFile = new ZipFile(zipFilePath)) {
    		final Enumeration<? extends ZipEntry> enu = zipFile.entries();
    		while (enu.hasMoreElements()) {

    			final ZipEntry zipEntry = enu.nextElement();
    			final String name = zipEntry.getName();
    			final File outputFile = new File(outputLocation + File.separator + name);

    			if (name.endsWith("/")) {
    				outputFile.mkdirs();
    				continue;
    			}

    			final File parent = outputFile.getParentFile();
    			if (parent != null) {
    				parent.mkdirs();
    			}

    			// Extract the file
    			try (final InputStream inputStream = zipFile.getInputStream(zipEntry);
    					final FileOutputStream outputStream = new FileOutputStream(outputFile)) {
    				/*
    				 * The buffer is the max amount of bytes kept in RAM during any given time while
    				 * unzipping. Since most windows disks are aligned to 4096 or 8192, we use a
    				 * multiple of those values for best performance.
    				 */
    				final byte[] bytes = new byte[8192];
    				while (inputStream.available() > 0) {
    					final int length = inputStream.read(bytes);
    					outputStream.write(bytes, 0, length);
    				}
    			}
    		}
    	}
    }
}
