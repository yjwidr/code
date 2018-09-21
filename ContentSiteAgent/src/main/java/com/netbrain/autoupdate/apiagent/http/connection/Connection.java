package com.netbrain.autoupdate.apiagent.http.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.netbrain.autoupdate.apiagent.proxy.ProxyClass;
import com.netbrain.autoupdate.apiagent.proxy.ProxySetting;
import com.netbrain.autoupdate.apiagent.utils.CommonUtils;
import com.netbrain.autoupdate.apiagent.utils.EncryptUtil;

@Component
public class Connection {
    @Value("${agent.read.timeout}")
    private int readTimeout;
    @Value("${agent.connect.timeout}")
    private int connectTimeout;

    private static Logger logger = LogManager.getLogger(Connection.class.getName());
    
    public HttpURLConnection connectHttp(String strUrl, ProxySetting proxySetting) throws IOException {
        URL url = new URL(strUrl);
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection(ProxyClass.getProxy(proxySetting));
        setRequestHeader(urlConn);
        return urlConn;
    }
    
    public HttpsURLConnection connectHttps(String strUrl, String certPath, ProxySetting proxySetting) throws IOException, KeyManagementException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException {
        HttpsURLConnection connection = null;
        SSLSocketFactory socFac = null;
        if(certPath == null) {
        	socFac = getSslSocketFactory();
        }else {
        	socFac = getSslSocketFactory(certPath);
        }
        connection = conn(strUrl, socFac, proxySetting);
        setRequestHeader(connection);
        return connection;
    }

    private HttpsURLConnection conn(String strUrl, SSLSocketFactory socFac, ProxySetting proxySetting) throws MalformedURLException, IOException {
        URL url = new URL(strUrl);
        HttpsURLConnection urlConn = (HttpsURLConnection) url.openConnection(ProxyClass.getProxy(proxySetting));
        setSSLForUrlConnection(urlConn, socFac);
        return urlConn;
    }
    
    private void setSSLForUrlConnection(HttpsURLConnection urlConn, SSLSocketFactory sslFactory) {
    	urlConn.setSSLSocketFactory(sslFactory);
        urlConn.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession sslsession) {
                return true;
            }
        });
    }

    private TrustManagerFactory getTrustManager(String path)
            throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        if (StringUtils.isEmpty(path)) {
            KeyStore ks = KeyStore.getInstance("Windows-ROOT");
            ks.load(null, null);
            trustManagerFactory.init(ks);
        } else {
            InputStream ins = null;
            try {
                ins = new FileInputStream(getCertFile(path));
                CertificateFactory cerFactory = CertificateFactory.getInstance("X.509");
                Certificate cer = cerFactory.generateCertificate(ins);
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry("trust", cer);
                trustManagerFactory.init(keyStore);
            }finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return trustManagerFactory;
    }
    
    private SSLSocketFactory getSslSocketFactory() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Couldn't create tls Context for  connection", e);
            throw new RuntimeException(e);
        }
        return sslContext.getSocketFactory();
    }

    private SSLSocketFactory getSslSocketFactory(String path) throws NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException, CertificateException, IOException {
        TrustManagerFactory trustManagerFactory = getTrustManager(path);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession sslsession) {
                return true;
            }
        });
        return sslContext.getSocketFactory();
    }

    private File getCertFile(String path) {
        File certFile = null;
        File dir = new File(path);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    certFile = file;
                    break;
                }
            }
        }
        return certFile;
    }
    private void setRequestHeader(URLConnection conn) throws ProtocolException {
    	conn.setReadTimeout(readTimeout * 1000);
    	conn.setConnectTimeout(connectTimeout * 1000);
    	conn.setRequestProperty("Charset", "UTF-8");
    	conn.setRequestProperty("Connection", "Keep-Alive");
    	conn.setDoOutput(true);
    	conn.setDoInput(true);
    	conn.setUseCaches(false);
    }
    public void setRequestAuth(String licenseId,String apiKey,URLConnection conn) throws Exception {
        if(conn!=null) {
            if(!StringUtils.isEmpty(licenseId)) {
            	conn.setRequestProperty("Authorization", "license "+CommonUtils.base64encode(licenseId.getBytes()));
            }
            if(apiKey != null) {
                conn.setRequestProperty("Apikey", EncryptUtil.aesEncrypt(apiKey, EncryptUtil.key));
            }
        }
    }   
}
