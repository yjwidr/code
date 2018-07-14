package com.netbrain.autoupdate.apiserver.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.netbrain.autoupdate.apiserver.exception.BusinessException;
import com.netbrain.autoupdate.apiserver.exception.ErrorCodes;
import com.netbrain.autoupdate.apiserver.services.ContentPackageService;

@Service
public class ContentPackageServiceImpl implements ContentPackageService{

	@Override
	public String unpackAndSave(MultipartFile file) {
		// TODO Auto-generated method stub
		return null;
	}


	public void Import(MultipartFile file) {
		if(file.isEmpty()) {
			throw new BusinessException(ErrorCodes.ERROR_FILE_IS_EMPTY);
		}
		String packageFileName = file.getOriginalFilename();
	}
	
	
}
