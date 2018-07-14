package com.netbrain.autoupdate.apiserver.services;

import java.util.List;

import com.netbrain.autoupdate.apiserver.model.ContentPackageEntity;
import com.netbrain.autoupdate.apiserver.model.ContentVersionEntity;
import com.netbrain.autoupdate.apiserver.model.parameter.UploadContent;
import com.netbrain.autoupdate.apiserver.model.parameter.UploadOneContent;

public interface ContentVersionService {
	/*
	 * 探测可以用于升级的包版本列表，包括disabled和deleted的版本，当前只返回支持的最后一个全量包范围内的版本列表
	 */
	List<ContentVersionEntity> getDetectVersions(String softwareVersion, String contentVersion, String licenseId);
	
	ContentPackageEntity downloadOnePackage(String softwareVersion, String contentVersion);
	
	List<ContentPackageEntity> downloadMultiPackage(String softwareVersion, String[] contentVersions);
	
	boolean uploadContentVersionForMultiSoftwareVersion(UploadContent content);
	
	ContentVersionEntity uploadContentVersion(UploadOneContent content, String packageId, String userId, String author);
	
	/*
	 * 0:major, 1:minor, 2:revision
	 */
	int[] getMaxContentVersion(String softwareVersion);
	
	/*
	 * 0:major, 1:minor
	 */
	int[] getMaxContentMinorVersion(String softwareVersion);
	
	List<ContentVersionEntity> getAllContentVersionList();
	
	int publishContentVersion(String[] ids, String userId);
	int disableContentVersion(String[] ids, String userId);
	
}
