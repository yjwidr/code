package com.netbrain.autoupdate.apiagent.config;

public abstract class BaseConfig {
	private boolean ssl;
    private String cert_verification;
    private String url;
    private int type;
    private String path;
    
    protected static final int CERTIFICATE_INSTATLL_TYPE_PREPARE = 0;
    protected static final String SSL_WITH_VERIFY_CERT = "verify_ca_no_limit";
    protected static final String AU_CERT_PATH = "conf/certs/AU";
    protected static final String IE_CERT_PATH = "conf/certs/IE";

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getCert_verification() {
        return cert_verification;
    }

    public void setCert_verification(String cert_verification) {
        this.cert_verification = cert_verification;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    public abstract String getCertPath();
}
