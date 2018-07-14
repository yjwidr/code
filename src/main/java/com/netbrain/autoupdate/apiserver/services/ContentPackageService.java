package com.netbrain.autoupdate.apiserver.services;

import org.springframework.web.multipart.MultipartFile;

public interface ContentPackageService {
	String unpackAndSave(MultipartFile file);
}
