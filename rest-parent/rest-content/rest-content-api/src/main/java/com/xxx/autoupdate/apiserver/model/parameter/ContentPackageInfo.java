package com.xxx.autoupdate.apiserver.model.parameter;

import java.io.Serializable;
import java.util.List;

public class ContentPackageInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int ContentPackageType;
	private String ContentPackageName;
	private String[] CompatibleSoftwareVersions;
	private List<ContentPackageVersionInfo> contentPackageVersions;
	
	public int getContentPackageType() {
		return ContentPackageType;
	}
	public void setContentPackageType(int contentPackageType) {
		ContentPackageType = contentPackageType;
	}
	public String getContentPackageName() {
		return ContentPackageName;
	}
	public void setContentPackageName(String contentPackageName) {
		ContentPackageName = contentPackageName;
	}
	public String[] getCompatibleSoftwareVersions() {
		return CompatibleSoftwareVersions;
	}
	public void setCompatibleSoftwareVersions(String[] compatibleSoftwareVersions) {
		CompatibleSoftwareVersions = compatibleSoftwareVersions;
	}
	public List<ContentPackageVersionInfo> getContentPackageVersions() {
		return contentPackageVersions;
	}
	public void setContentPackageVersions(List<ContentPackageVersionInfo> contentPackageVersions) {
		this.contentPackageVersions = contentPackageVersions;
	}
}
