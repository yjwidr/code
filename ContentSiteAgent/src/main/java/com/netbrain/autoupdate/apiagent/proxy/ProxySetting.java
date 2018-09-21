package com.netbrain.autoupdate.apiagent.proxy;

import java.io.Serializable;

public class ProxySetting implements Serializable {
	private static final long serialVersionUID = 1L;
	public boolean enableProxy;
	private String proxyAddress;
	private int proxyPort;
	private String proxyUsername;
	private String proxyPassword;
	public boolean isEnableProxy() {
		return enableProxy;
	}
	public void setEanbleProxy(boolean enableProxy) {
		this.enableProxy = enableProxy;
	}
	public String getProxyAddress() {
		return proxyAddress;
	}
	public void setProxyAddress(String proxyAddress) {
		this.proxyAddress = proxyAddress;
	}
	public int getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	public String getProxyUsername() {
		return proxyUsername;
	}
	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}
	public String getProxyPassword() {
		return proxyPassword;
	}
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
}
