package com.netbrain.autoupdate.apiserver.services;

import java.util.List;

import com.netbrain.autoupdate.apiserver.model.parameter.WhiteListUser;

public interface WhiteListService {
	
	List<WhiteListUser> getWhiteList(String contentVersionId);
	
	/*
	 * 返回的是服务端生成的白名单用户id
	 */
	WhiteListUser AddWhiteList(WhiteListUser addUser);
	
	void UpdateWhiteList(WhiteListUser addUser);
	
	void DeleteWhiteList(List<String> ids);
}
