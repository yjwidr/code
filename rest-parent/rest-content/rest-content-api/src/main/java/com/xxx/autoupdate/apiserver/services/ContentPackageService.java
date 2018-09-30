package com.xxx.autoupdate.apiserver.services;

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

import com.xxx.autoupdate.apiserver.model.parameter.PackageInfo;
import com.xxx.autoupdate.apiserver.model.parameter.PackageVersionParameter;

public interface ContentPackageService {
	PackageInfo unpack(MultipartFile file, Path tempZipFile, String packageFolderPath);
	PackageInfo getPackageSummary(PackageVersionParameter packageInfo);
	void pack(String packageFolderPath, Path packageFolderZip, PackageInfo packageInfo, String packagePack );
	byte[] pack2(byte[] data, PackageInfo packageInfo);
	byte[] getPackageBytes(MultipartFile file);
}
