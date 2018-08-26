package com.xxx.autoupdate.apiserver.model.parameter;

import java.io.Serializable;

public class ContentPackageVersionInfo  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String forSoftwareVersion;
	private ContentVersion forContentVersion;
	private ContentVersion contentPackageVersion;
	public String getForSoftwareVersion() {
		return forSoftwareVersion;
	}
	public void setForSoftwareVersion(String forSoftwareVersion) {
		this.forSoftwareVersion = forSoftwareVersion;
	}
	public ContentVersion getForContentVersion() {
		return forContentVersion;
	}
	public void setForContentVersion(ContentVersion forContentVersion) {
		this.forContentVersion = forContentVersion;
	}
	public ContentVersion getContentPackageVersion() {
		return contentPackageVersion;
	}
	public void setContentPackageVersion(ContentVersion contentPackageVersion) {
		this.contentPackageVersion = contentPackageVersion;
	}

}
