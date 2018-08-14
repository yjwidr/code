package com.xxx.autoupdate.apiserver.util;


import com.xxx.autoupdate.apiserver.exception.BusinessException;
import com.xxx.autoupdate.apiserver.exception.ErrorCodes;

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
			throw new BusinessException(ErrorCodes.ERROR_CONTENT_VERSION_INVALID);
		}
		int major, minor, revision;
		try {
			major = Integer.parseInt(nums[0]);
			minor = Integer.parseInt(nums[1]);
			revision = Integer.parseInt(nums[2]);
		}catch(NumberFormatException e) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENT_VERSION_INVALID);
		}
		return convertContentVersion(major, minor, revision);
	}
	
	public static long convertContentVersion(int major, int minor, int revision) {
		long result = (long)major * MILLION * THOUSAND + minor * MILLION + revision;
		return result;
	}
	public static long convertContentVersion(int[] versions) {
		long result = (long)versions[0] * MILLION * THOUSAND + versions[1] * MILLION + versions[2];
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
		if(lastConventVersion == 0) {
			return new int[] {1,1,0};
		}
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
