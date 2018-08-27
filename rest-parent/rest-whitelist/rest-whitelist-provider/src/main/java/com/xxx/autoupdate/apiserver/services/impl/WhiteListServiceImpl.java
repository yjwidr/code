package com.xxx.autoupdate.apiserver.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xxx.autoupdate.apiserver.dao.IEUserInfoRepository;
import com.xxx.autoupdate.apiserver.dao.WhiteListRepository;
import com.xxx.autoupdate.apiserver.exception.BusinessException;
import com.xxx.autoupdate.apiserver.exception.ErrorCodes;
import com.xxx.autoupdate.apiserver.model.IEUserInfoEntity;
import com.xxx.autoupdate.apiserver.model.WhiteListEntity;
import com.xxx.autoupdate.apiserver.model.parameter.WhiteListUser;
import com.xxx.autoupdate.apiserver.services.WhiteListService;
@Service
public class WhiteListServiceImpl implements WhiteListService {
	@Autowired
	private WhiteListRepository whiteListRepository;
	
	@Autowired
	private IEUserInfoRepository ieUserInfoRepository;

	@Override
	public List<WhiteListUser> getWhiteList(String contentVersionId) {
		List<WhiteListUser> result = new ArrayList<WhiteListUser>();
		
		List<WhiteListEntity> wls = whiteListRepository.getWhiteListByContentVersionId(contentVersionId);
		if(wls.size()==0) {
			return result;
		}
		List<String> lcslists = new ArrayList<String>();
		for(WhiteListEntity wl : wls) {
			lcslists.add(wl.getLicenseId());
		}
		List<IEUserInfoEntity> ieUsers = ieUserInfoRepository.getByLicenseIds(lcslists.toArray(new String[lcslists.size()]));
		Map<String, IEUserInfoEntity> map = new HashMap<String, IEUserInfoEntity>();
		for(IEUserInfoEntity ieUser : ieUsers) {
			if(!map.containsKey(ieUser.getLicenseId())) {
				map.put(ieUser.getLicenseId(), ieUser);
			}
		}
		
		for(WhiteListEntity wl : wls) {
			IEUserInfoEntity ieUser = map.get(wl.getLicenseId());
			WhiteListUser u = new WhiteListUser();
			u.setId(wl.getId());
			u.setLicenseId(wl.getLicenseId());
			if(ieUser != null) {
				u.setUserName(ieUser.getUserName());
				u.setEmail(ieUser.getEmail());
				u.setCompany(ieUser.getEmail());
				u.setDescription(ieUser.getDescription());
			}
			result.add(u);
		}
		return result;
	}

	/*
	 * 返回的是服务端生成的白名单用户id
	 * @see com.xxx.autoupdate.apiserver.services.WhiteListService#AddWhiteList(com.xxx.autoupdate.apiserver.model.parameter.WhiteListUser)
	 */
	@Transactional(rollbackOn=Exception.class)
	@Override
	public WhiteListUser AddWhiteList(WhiteListUser addUser) {
		String licenseId = addUser.getLicenseId();
		upsertIEuserInfo(addUser, licenseId);
		
		String contentVersionId = addUser.getContentVersionId();
		WhiteListEntity wlEntity = new WhiteListEntity();
		wlEntity.setContentVersionId(contentVersionId);
		wlEntity.setLicenseId(licenseId);
		wlEntity = whiteListRepository.save(wlEntity);
		addUser.setId(wlEntity.getId());
		return addUser;
	}
	/*
	 * 根据LicenseId修改IEUserInfo，同时修改WhiteList的licenseId
	 * @see com.xxx.autoupdate.apiserver.services.WhiteListService#UpdateWhiteList(com.xxx.autoupdate.apiserver.model.parameter.WhiteListUser)
	 */
	@Transactional(rollbackOn=Exception.class)
	@Override
	public void UpdateWhiteList(WhiteListUser addUser) {
		String id = addUser.getId();
		WhiteListEntity wlEntity = whiteListRepository.getOne(id);
		if(wlEntity == null) {
			throw new BusinessException(ErrorCodes.ERROR_WHITE_LIST_ID_NOT_EXISTS);
		}
		String licenseId = addUser.getLicenseId();
		upsertIEuserInfo(addUser, licenseId);
		
		wlEntity.setLicenseId(licenseId);
		whiteListRepository.save(wlEntity);
	}

	private void upsertIEuserInfo(WhiteListUser addUser, String licenseId) {
		IEUserInfoEntity ieUserEntity = ieUserInfoRepository.getByLicenseId(licenseId);
		if(ieUserEntity == null) {
			ieUserEntity = new IEUserInfoEntity();
		}
		ieUserEntity.setLicenseId(licenseId);
		ieUserEntity.setUserName(addUser.getUserName());
		ieUserEntity.setEmail(addUser.getEmail());
		ieUserEntity.setCompany(addUser.getCompany());
		ieUserEntity.setDescription(addUser.getDescription());
		ieUserInfoRepository.save(ieUserEntity);
	}
	
	@Transactional(rollbackOn=Exception.class)
	@Override
	public void DeleteWhiteList(List<String> ids) {
		List<WhiteListEntity> wlList = whiteListRepository.findAllById(ids);
		whiteListRepository.deleteAll(wlList);
	}
	
	
	
}
