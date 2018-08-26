package com.xxx.autoupdate.apiserver.services;

import java.util.List;

import com.xxx.autoupdate.apiserver.model.ContentPackageEntity;
import com.xxx.autoupdate.apiserver.model.ContentVersionEntity;
import com.xxx.autoupdate.apiserver.model.parameter.UploadContent;
import com.xxx.autoupdate.apiserver.model.parameter.UploadOneContent;

public interface ContentVersionService {
	/*
	 * 探测可以用于升级的包版本列表，包括disabled和deleted的版本，当前只返回支持的最后一个全量包范围内的版本列表
	 */
	List<ContentVersionEntity> getDetectVersions(String softwareVersion, String contentVersion, String licenseId);
	
	ContentPackageEntity downloadOnePackage(String softwareVersion, String contentVersion);
	
	List<ContentPackageEntity> downloadMultiPackage(String softwareVersion, String[] contentVersions);
	
	boolean uploadContentVersionForMultiSoftwareVersion(UploadContent content);
	
	void uploadContentVersion(UploadOneContent content, String userId);
	void uploadContentVersion2(UploadOneContent content, String userId);
	
	/**
	 * 0:major, 1:minor, 2:revision
	 */
	int[] getLastContentVersionOfSpacialPkg(String softwareVersion, int majorVersion, int minorVersion);
	
	/**
	 * 0:major, 1:minor, 2:revision
	 */
	int[] getMaxContentVersion(String softwareVersion);
	/**
	 * 0:major, 1:minor, 2:0
	 */
	int[] getMaxContentMinorVersion(String softwareVersion);
	
	boolean existMinorContentVersion(String softwareVersion, long contentVersion);
	
	List<ContentVersionEntity> getAllContentVersionList();
	
	int publishContentVersion(String[] ids, String userId);
	int disableContentVersion(String[] ids, String userId);
	int updateContentVersionNameAndDesc(String id, String name, String description, String userId);
	
}
