package com.xxx.autoupdate.apiserver.model.constant;

public class ContentPackageType {
	/*
	 * 全量包：包含该版本的所有资源，在IE中存在的而不在该包中存在的Resource在升级时视为需要删除
	 */
	public static final int WholePackage = 1;
	/*
	 * 补丁包：基于全量包的补丁
	 */
	public static final int PatchPackage = 2;
}
