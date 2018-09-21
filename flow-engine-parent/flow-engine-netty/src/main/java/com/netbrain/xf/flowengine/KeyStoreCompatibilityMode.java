package com.netbrain.xf.flowengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;

public class KeyStoreCompatibilityMode {

    public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, NoSuchProviderException {
        System.out.println(System.getenv("JAVA_HOME")+File.separator+"jre"+File.separator+"lib"+File.separator+"security"+File.separator+"cacerts");
        String cacerts=System.getenv("JAVA_HOME")+File.separator+"jre"+File.separator+"lib"+File.separator+"security"+File.separator+"cacerts";
//        KeyStore ks = KeyStore.getInstance("Windows-ROOT");
        KeyStore ks = KeyStore.getInstance(new File(cacerts),"changeit".toCharArray());
        Enumeration en = ks.aliases();
        while (en.hasMoreElements()) {
            String aliasKey = (String) en.nextElement();
            Certificate c = ks.getCertificate(aliasKey);
            System.out.println("---> alias : " + aliasKey);
            if(aliasKey.equals("rootca")) {
                if (ks.isCertificateEntry(aliasKey)) {
                    Certificate[] chain =  ks.getCertificateChain(aliasKey);
                    System.out.println("---> chain length: " + chain.length);
                    for (Certificate cert : chain) {
                        System.out.println(cert.getPublicKey());
                    }
                }
            }
        }
    }
}