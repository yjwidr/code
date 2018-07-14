package com.netbrain.autoupdate.apiserver.util;


import com.netbrain.autoupdate.apiserver.exception.BusinessException;
import com.netbrain.autoupdate.apiserver.exception.ErrorCodes;

public class ContentVersionConvert {
	public static final int MILLION = 1000000;
	public static final int THOUSAND = 1000;
	public static final short CONTENTVERSIONWHOLEDTYPE = 1 ; 
	public static final int WHOLEDREVISION = 0 ; 
	
	/*
	 * 转换失败抛异常
	 */
	public static long contentVersionFromStrToLong(String contentVersionStr) {
		String[] nums = contentVersionStr.split("\\.");
		if(nums.length != 3) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_ERROR);
		}
		int major, minor, revision;
		try {
			major = Integer.parseInt(nums[0]);
			minor = Integer.parseInt(nums[1]);
			revision = Integer.parseInt(nums[2]);
		}catch(NumberFormatException e) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_ERROR);
		}
		return convertContentVersion(major, minor, revision);
	}
	
	public static long convertContentVersion(int major, int minor, int revision) {
		long result = major * MILLION * THOUSAND + minor * MILLION + revision;
		if(result ==0) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_ERROR);
		}
		return result;
	}
	
	/*
	 * 0：major, 1:minor, 2:revision
	 */
	public static int[] contentVersionFromLongToSplit(long contentVersionLong) {
		int revision = (int) (contentVersionLong % MILLION);
		long remain = contentVersionLong / MILLION;
		int minor = (int) (remain % THOUSAND);
		int major = (int) (remain / THOUSAND);
		return new int[] {major, minor, revision};
	}
	public static String contentVersionFromLongToStr(long contentVersionLong) {
		int[] vers = contentVersionFromLongToSplit(contentVersionLong);
		return String.format("%d.%d.%d", vers[0],vers[1],vers[2]);
	}
	
	
	public static int[] addContentVersionForSplit(long lastConventVersion, short type) {
		int[] versions = ContentVersionConvert.contentVersionFromLongToSplit(lastConventVersion);
		int major = versions[0];
		int minor = versions[1];
		int revision = versions[2];
		if(type == CONTENTVERSIONWHOLEDTYPE) {
			minor += 1;
			revision = WHOLEDREVISION;
		} else {
			revision += 1;
		}
		if(revision == MILLION) {
			minor += 1;
			revision = WHOLEDREVISION + 1;
		}
		if(minor == THOUSAND) {
			minor = 1;
			major += 1;
		}
		return new int[] {major, minor, revision};
	}
	public static long addContentVersion(long lastConventVersion, short type) {
		int[] versions = addContentVersionForSplit(lastConventVersion, type);
		return convertContentVersion(versions[0], versions[1], versions[2]);
	}
}
