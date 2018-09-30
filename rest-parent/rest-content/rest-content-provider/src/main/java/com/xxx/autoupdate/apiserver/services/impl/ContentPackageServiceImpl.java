package com.xxx.autoupdate.apiserver.services.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.dubbo.config.annotation.Service;
import com.xxx.autoupdate.apiserver.exception.BusinessException;
import com.xxx.autoupdate.apiserver.exception.ErrorCodes;
import com.xxx.autoupdate.apiserver.model.parameter.PackageInfo;
import com.xxx.autoupdate.apiserver.model.parameter.PackageInventory;
import com.xxx.autoupdate.apiserver.model.parameter.PackageSummary;
import com.xxx.autoupdate.apiserver.model.parameter.PackageVersionParameter;
import com.xxx.autoupdate.apiserver.services.ContentPackageService;
import com.xxx.autoupdate.apiserver.util.CommonUtils;

@Service(interfaceClass=ContentPackageService.class)
@Component
public class ContentPackageServiceImpl implements ContentPackageService{
	private static Logger logger = LogManager.getLogger(ContentPackageServiceImpl.class.getName());
	/**
	 * originalZipPackage 原有包中的zip文件
	 */
	@Override
	public PackageInfo unpack(MultipartFile file, Path originalZipPackage, String packageFolderPath) {
		if(file.isEmpty()) {
			throw new BusinessException(ErrorCodes.ERROR_FILE_IS_EMPTY);
		}
		try {
			//存储临时文件
			byte[] b = this.getPackageBytes(file);
			Files.write(originalZipPackage, b, StandardOpenOption.WRITE);
			//解包	
			CommonUtils.unzip(originalZipPackage.toString(), packageFolderPath);
			String tempPackageInfoFile = packageFolderPath + File.separator + "package.json";
			byte[] bytes = Files.readAllBytes(Paths.get(tempPackageInfoFile));
			PackageInfo packageInfo = CommonUtils.convertJsonBytesToObject(bytes, PackageInfo.class);
			return packageInfo;
			
		} catch (IOException e) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_UPLOAD_ERROR);
		}
		
	}
	
	@Override
	public byte[] pack2(byte[] data, PackageInfo packageInfo) {
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			//0. 4 Byte 包版本号
			baos.write(CommonUtils.convertIntToByte(1));
			//1. 4 Byte 摘要块大小
			PackageSummary packageSummary = new PackageSummary();
			packageSummary.loadFrom(packageInfo);
			byte[] summaryData = CommonUtils.convertObjectToJsonBytes(packageSummary);
			baos.write(CommonUtils.convertIntToByte(summaryData.length));
			//2. 4 Byte 清单块大小
			PackageInventory packageInventory = new PackageInventory();
			packageInventory.setResources(packageInfo.getResources());
			byte[] inventoryData = CommonUtils.convertObjectToJsonBytes(packageInventory);
			baos.write(CommonUtils.convertIntToByte(inventoryData.length));
			//3. 4 Byte zip块大小
			baos.write(CommonUtils.convertIntToByte(data.length));
			baos.flush();
			//4. x Byte 摘要数据块
			baos.write(summaryData);
			baos.flush();
			//5. y Byte 清单数据块
			baos.write(inventoryData);
			baos.flush();
			//6. z Byte zip数据块
			baos.write(data);
			baos.flush();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_UPLOAD_ERROR);
		}
	}
	
	@Override
	public void pack(String packageFolderPath, Path packageFolderZip, PackageInfo packageInfo, String packagePack ) {
		
		try(FileOutputStream fos = new FileOutputStream(packagePack)) {
			//0. 4 Byte 包版本号
			fos.write(CommonUtils.convertIntToByte(1));
			//1. 4 Byte 摘要块大小
			PackageSummary packageSummary = new PackageSummary();
			packageSummary.loadFrom(packageInfo);
			byte[] summaryData = CommonUtils.convertObjectToJsonBytes(packageSummary);
			fos.write(CommonUtils.convertIntToByte(summaryData.length));
			//2. 4 Byte 清单块大小
			PackageInventory packageInventory = new PackageInventory();
			packageInventory.setResources(packageInfo.getResources());
			byte[] inventoryData = CommonUtils.convertObjectToJsonBytes(packageInventory);
			fos.write(CommonUtils.convertIntToByte(inventoryData.length));
			//3. 4 Byte zip块大小
			CommonUtils.zip(packageFolderPath, packageFolderZip.toString());
			packageFolderZip = Paths.get(packageFolderZip.toString());
			byte[] data = Files.readAllBytes(packageFolderZip);
			fos.write(CommonUtils.convertIntToByte(data.length));
			fos.flush();
			//4. x Byte 摘要数据块
			fos.write(summaryData);
			fos.flush();
			//5. y Byte 清单数据块
			fos.write(inventoryData);
			fos.flush();
			//6. z Byte zip数据块
			fos.write(data);
			fos.flush();
		} catch (IOException e) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_UPLOAD_ERROR);
		}
	}


	@Override
	public PackageInfo getPackageSummary(PackageVersionParameter packageInfo) {
		String base64Str = packageInfo.getBase64Data();
		return extractSummaryFromBase64(base64Str);
	}
	
	private PackageInfo extractSummaryFromBytes(ByteArrayInputStream targetReader) {
		try{
			PackageInfo summary = new PackageInfo();
			byte[] b = new byte[4];
			targetReader.read(b);
			int packageVersion  = CommonUtils.convertByteToInt(b);
			targetReader.read(b);
			int summaryDataLength  = CommonUtils.convertByteToInt(b);
			targetReader.read(b);
			int inventoryDataLength  = CommonUtils.convertByteToInt(b);	
			targetReader.read(b);
			int zipDataLength  = CommonUtils.convertByteToInt(b);
			if(zipDataLength > 0) {
				//摘要数据块
				b = new byte[summaryDataLength];
				int tp = targetReader.read(b);
				if(tp != -1) {
					summary = CommonUtils.convertJsonBytesToObject(b, PackageInfo.class);
				}
				//日来资源清单数据块
				b = new byte[inventoryDataLength];
				tp = targetReader.read(b);
				if(tp != -1) {
					PackageInventory inventory =  CommonUtils.convertJsonBytesToObject(b, PackageInventory.class);
					if(inventory != null) {
						summary.setResources(inventory.getResources());
					}
				}
				summary.setZipDataSize(Optional.of(zipDataLength));
			}
			summary.setPackageVersion(packageVersion);
			return summary;
		} catch (IOException e) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENT_INVENTORY);
		}
	}
	
	private PackageInfo extractSummaryFromBase64(String base64Str) {
		try {
			byte[] asBytes = CommonUtils.getFromBase64(base64Str);
			try(ByteArrayInputStream targetReader = new ByteArrayInputStream(asBytes))
			{
				PackageInfo summary = extractSummaryFromBytes(targetReader);
				return summary;
			}
		} catch (IOException e) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENT_INVENTORY);
		}
	}
	@Override
	public byte[] getPackageBytes(MultipartFile file) {
		if(file.isEmpty()) {
			throw new BusinessException(ErrorCodes.ERROR_FILE_IS_EMPTY);
		}
		try {
			byte[] fileBytes = file.getBytes();
			try(ByteArrayInputStream targetReader = new ByteArrayInputStream(fileBytes)){
				PackageInfo pi = this.extractSummaryFromBytes(targetReader);
				Optional<Integer> size =pi.getZipDataSize();
				int dataLength = 0;
				if(size.isPresent()) {
					dataLength = size.get();
				}
				byte[] b = new byte[dataLength];
				targetReader.read(b);
				return b;
			}
		} catch (IOException e) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_UPLOAD_ERROR);
		}
	}
	
	
}
