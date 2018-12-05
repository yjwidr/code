package com.xxx.autoupdate.apiserver.model.parameter;

import java.io.Serializable;

public class ServiceFileZipInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String serviceName;
	private String zipPath;
	private String extractToRelativePath;

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getZipPath() {
		return zipPath;
	}

	public void setZipPath(String zipPath) {
		this.zipPath = zipPath;
	}

	public String getExtractToRelativePath() {
		return extractToRelativePath;
	}

	public void setExtractToRelativePath(String extractToRelativePath) {
		this.extractToRelativePath = extractToRelativePath;
	}
}
