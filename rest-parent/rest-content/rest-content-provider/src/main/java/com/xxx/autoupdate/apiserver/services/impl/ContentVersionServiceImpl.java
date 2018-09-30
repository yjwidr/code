package com.xxx.autoupdate.apiserver.services.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.dubbo.config.annotation.Service;
import com.xxx.autoupdate.apiserver.dao.ContentPackageRepository;
import com.xxx.autoupdate.apiserver.dao.ContentVersionRepository;
import com.xxx.autoupdate.apiserver.dao.ResourceRepository;
import com.xxx.autoupdate.apiserver.dao.WhiteListRepository;
import com.xxx.autoupdate.apiserver.exception.BusinessException;
import com.xxx.autoupdate.apiserver.exception.ErrorCodes;
import com.xxx.autoupdate.apiserver.model.ContentPackageEntity;
import com.xxx.autoupdate.apiserver.model.ContentVersionEntity;
import com.xxx.autoupdate.apiserver.model.ResourceEntity;
import com.xxx.autoupdate.apiserver.model.parameter.ContentPackageInfo;
import com.xxx.autoupdate.apiserver.model.parameter.ContentPackageVersionInfo;
import com.xxx.autoupdate.apiserver.model.parameter.ContentVersion;
import com.xxx.autoupdate.apiserver.model.parameter.MainResourceItem;
import com.xxx.autoupdate.apiserver.model.parameter.PackageInfo;
import com.xxx.autoupdate.apiserver.model.parameter.ResourceItem;
import com.xxx.autoupdate.apiserver.model.parameter.UploadContent;
import com.xxx.autoupdate.apiserver.model.parameter.UploadOneContent;
import com.xxx.autoupdate.apiserver.services.ContentPackageService;
import com.xxx.autoupdate.apiserver.services.ContentVersionService;
import com.xxx.autoupdate.apiserver.util.CommonUtils;
import com.xxx.autoupdate.apiserver.util.ContentVersionConvert;
import com.xxx.autoupdate.apiserver.util.TwoTuple;
@Service(interfaceClass=ContentVersionService.class)
@Component
public class ContentVersionServiceImpl implements ContentVersionService {
	private static Logger logger = LogManager.getLogger(ContentVersionServiceImpl.class.getName());
	@Autowired
	private ContentVersionRepository contentVersionRepository;
	
	@Autowired
	private WhiteListRepository whiteListRepository;
	
	@Autowired
	private ContentPackageRepository contentPackageRepository;
	
	@Autowired
	private ResourceRepository resourceRepository;
	
	@Autowired
	private ContentPackageService contentPackageService;
	
	private static final short ACTIVATED = 1 ;
	private static final short DISABLED = 2 ;
	private static final short CHECKING = 1 ;
	private static final short PUBLISHED = 2 ;
	private static final String CONTENTVERSIONSPLITCHAR = "-" ; 
	private static final String CONTENTVERSIONINCLUDECHAR = "$" ; 

	private static final short CONTENTVERSIONCATEGORY = 1 ; 

	/*
	 * 探测可以用于升级的包版本列表，包括disabled和deleted的版本，当前只返回支持的最后一个全量包范围内的版本列表
	 * 最后一个包必须在可用状�
	 * @see com.xxx.autoupdate.apiserver.services.ContentVersionService#getDetectVersions(java.lang.String, long, String)
	 */
	@Override
	public List<ContentVersionEntity> getDetectVersions(String softwareVersion, String contentVersion, String licenseId){
		boolean hasLicense = licenseId != null && licenseId != "";
		long contentVersionLong = ContentVersionConvert.contentVersionFromStrToLong(contentVersion);
		List<Object[]> lists = contentVersionRepository.getGTBySoftVersionIdAndContentVersion(softwareVersion, contentVersionLong);
		List<ContentVersionEntity> list = toContentVersionEntity(lists);
		List<ContentVersionEntity> result = new ArrayList<ContentVersionEntity>();
		boolean notUsed = true; //去除开始的disabled,deleted 或非check user的checking的version.
		for(ContentVersionEntity entity: list) {
			if(notUsed) {
				if(entity.getActivateStatus() != ACTIVATED) {
					continue;
				}
				boolean isWhite = false; //是否是白名单用户
				if(hasLicense) {
					String lcsId = whiteListRepository.getWhiteListByContentVersionId(entity.getId(), licenseId);
					isWhite= lcsId != null && lcsId !="";
				}
				if(!isWhite && entity.getPublishStatus() != PUBLISHED) {
					continue;
				}
				notUsed = false;
			}
			result.add(entity);
			if(entity.getRevisionVersion() == ContentVersionConvert.WHOLEDREVISION) {
				break;
			}
		}
		return result;
	}
	
	/*
	 * 下载特定版本
	 * @see com.xxx.autoupdate.apiserver.services.ContentVersionService#downloadOnePackage(java.lang.String, java.lang.String)
	 */
	@Override
	public ContentPackageEntity downloadOnePackage(String softwareVersion, String contentVersion) {
		long contentVersionLong = ContentVersionConvert.contentVersionFromStrToLong(contentVersion);
		String packageId = contentVersionRepository.getContentPackageIdByVersion(softwareVersion, contentVersionLong);
		ContentPackageEntity cpEntity = CommonUtils.toObject(contentPackageRepository.findById(packageId));
		if(cpEntity!=null) {
			cpEntity.setVersion( contentVersion+ "@" + softwareVersion);
			return cpEntity;
		}else {
			return null;
		}
	}

	/*
	 * 下载多个版本
	 * @see com.xxx.autoupdate.apiserver.services.ContentVersionService#downloadMultiPackage(java.lang.String, java.lang.String[])
	 */
	@Override
	public List<ContentPackageEntity> downloadMultiPackage(String softwareVersion, String[] contentVersions){
		TwoTuple<List<Long>, List<TwoTuple<Long,Long>>> vers = convertContentVersions(contentVersions);
		List<Long> downloadVers = vers.first;
		List<TwoTuple<Long,Long>> rangeVers = vers.second;
		TwoTuple<Long,Long> range = rangeVers.remove(0);
		long from = range.first;
		long to = range.second;
		List<String> downloadPackages = new ArrayList<String>();
		Map<String, Long> packageVersion = new HashMap<String, Long>();
		if(from !=Long.MAX_VALUE && to != 0) {
			List<Object[]> lists = contentVersionRepository.getGTEBySoftVersionIdAndContentVersion(softwareVersion, from);
			List<ContentVersionEntity> list = toContentVersionEntity(lists);
			List<TwoTuple<Long,String>> inRangeVers = new ArrayList<TwoTuple<Long,String>>();
			//筛选需要的contentVersionId �packageId
			for(ContentVersionEntity entity: list) {
				long thisVer = entity.getContentVersion();
				if(thisVer > to) { //list降序排列
					continue;
				}
				if(thisVer < from) {
					break;
				}
				String packageId = entity.getPackageId();
				if(downloadVers.contains(thisVer)) {
					if(!downloadPackages.contains(packageId)) {
						downloadPackages.add(packageId);
						packageVersion.put(packageId, thisVer);
					}
				}else {
					inRangeVers.add(new TwoTuple<Long,String>(thisVer,packageId));
				}
			}
			//根据from/to 筛选出所有packageId
			for(TwoTuple<Long,Long> rangVer : rangeVers) {
				from = rangVer.first;
				to = rangVer.second;
				for(TwoTuple<Long,String> innerVer:inRangeVers) {
					if(innerVer.first >= to) { //inRangeVers降序排列
						continue;
					}
					if(innerVer.first <= from) {
						break;
					}
					if(!downloadPackages.contains(innerVer.second)) {
						downloadPackages.add(innerVer.second);
						packageVersion.put(innerVer.second, innerVer.first);
					}
				}
			}
		}
		List<ContentPackageEntity> packages = new ArrayList<ContentPackageEntity>();
		if(downloadPackages.size() > 0) {
			packages = contentPackageRepository.findAllById(downloadPackages);
			for(ContentPackageEntity cp : packages) {
				cp.setVersion(ContentVersionConvert.contentVersionFromLongToStr(packageVersion.get(cp.getId()))+ "@"+ softwareVersion);
			}
		}
		return packages;
	}
	
	/*
	 * 提取出肯定包含的contentVersion以及需要查找范围的contentVersion range，并给出最终的search范围（保存在rangeVers的第一个位置）
	 */
	private TwoTuple<List<Long>, List<TwoTuple<Long,Long>>> convertContentVersions(String[] contentVersions) {
		long minVer = Long.MAX_VALUE;
		long maxVer = 0;
		List<Long> cntVersions = new ArrayList<Long>();
		List<TwoTuple<Long,Long>> rangeVers = new ArrayList<TwoTuple<Long,Long>>();
		for(String contentVersion : contentVersions) {
			if(contentVersion.indexOf(CONTENTVERSIONSPLITCHAR) >= 0) {
				String[] versions = contentVersion.split(CONTENTVERSIONSPLITCHAR);
				if(versions.length < 2) {
					throw new BusinessException(ErrorCodes.ERROR_CONTENT_VERSION_INVALID);
				}
				long fromVer = ContentVersionConvert.contentVersionFromStrToLong(versions[0].replace(CONTENTVERSIONINCLUDECHAR, ""));
				if(versions[0].endsWith(CONTENTVERSIONINCLUDECHAR)) {
					if(!cntVersions.contains(fromVer)) {
						cntVersions.add(fromVer);
					}
				}
				minVer = Math.min(minVer, fromVer);
				long toVer = ContentVersionConvert.contentVersionFromStrToLong(versions[1].replace(CONTENTVERSIONINCLUDECHAR, ""));
				if(versions[1].endsWith(CONTENTVERSIONINCLUDECHAR)) {
					if(!cntVersions.contains(toVer)) {
						cntVersions.add(toVer);
					}
				}
				maxVer = Math.max(maxVer, toVer);
				rangeVers.add(new TwoTuple<Long,Long>(fromVer,toVer));
			}else {
				long cntVersion = ContentVersionConvert.contentVersionFromStrToLong(contentVersion);
				minVer = Math.min(minVer, cntVersion);
				maxVer = Math.max(maxVer, cntVersion);
				if(!cntVersions.contains(cntVersion)) {
					cntVersions.add(cntVersion);
				}
			}
		}
		rangeVers.add(0, new TwoTuple<Long,Long>(minVer,maxVer));
		return new TwoTuple<List<Long>, List<TwoTuple<Long,Long>>>(cntVersions, rangeVers);
	}
	/*
	 * 暂时没实现完
	 * @see com.xxx.autoupdate.apiserver.services.ContentVersionService#uploadContentVersions(com.xxx.autoupdate.apiserver.model.parameter.UploadContent)
	 */
	@Transactional(rollbackOn=Exception.class)
	@Override
	public boolean uploadContentVersionForMultiSoftwareVersion(UploadContent content) {
		ContentVersionEntity cv = new ContentVersionEntity();
		cv.setName(content.getContentName());
		cv.setCategory(CONTENTVERSIONCATEGORY);
		cv.setType(content.getContentPackageType());
		cv.setPublishStatus(CHECKING);
		cv.setActivateStatus(ACTIVATED);
		cv.setDescription(content.getDescription());
		String[] softwareVersions = content.getSupportSoftwareVersions();
		for(String softwareVersion : softwareVersions) {
			cv.setSoftwareVersion(softwareVersion);
			int[] maxVersions = this.getMaxContentVersion(softwareVersion);
			long maxVersion = ContentVersionConvert.convertContentVersion(maxVersions);
			int[] versions = ContentVersionConvert.addContentVersionForSplit(maxVersion, content.getContentPackageType());
			cv.setMajorVersion(versions[0]);
			cv.setMinorVersion(versions[1]);
			cv.setRevisionVersion(versions[2]);
			cv.setContentVersion(ContentVersionConvert.convertContentVersion(versions[0], versions[1], versions[2]));
		}
		return false;
	}
	@Override
	public void uploadContentVersion2(UploadOneContent content, String userId) {

		try {
			byte[] origialBytes = content.getBytes(); //返回zip块 bytes
			TwoTuple<byte[], PackageInfo> tup = changePackageJsonInfo(origialBytes, content); //origialBytes;//
			byte[] data = contentPackageService.pack2(tup.first, tup.second);
			addContentVersionToDB(content, userId, tup.second, data);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_UPLOAD_ERROR);
		}
	}
	private TwoTuple<byte[], PackageInfo> changePackageJsonInfo(byte[] zippedBytes, UploadOneContent content) throws Exception {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PackageInfo pi = null;
		try {
			try(
				ZipOutputStream out = new ZipOutputStream(baos);
				ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zippedBytes))){
				    ZipEntry zipEntry;
				    try {
				        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				        	String entryName = zipEntry.getName();
			            	byte[] bytes = readByteArray(zipInputStream);
				            if("package.json".equals(entryName)) {
				            	pi = CommonUtils.convertJsonBytesToObject(bytes, PackageInfo.class);
				            	if(pi.getDescription()==null) {
				            		pi.setDescription(content.getDescription());
				            	}
				            	setContentPackageInfo(pi, content);
				            	bytes = CommonUtils.convertObjectToJsonBytes(pi);
				            }
				            ZipEntry ze = new ZipEntry(entryName);
				            ze.setSize(bytes.length);
				            out.putNextEntry(ze);
				            out.write(bytes);
				            out.closeEntry();
			                zipInputStream.closeEntry();
				        }
				    } catch (IOException e) {
						logger.error(e.getMessage(),e);
						throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_UPLOAD_ERROR);
				    }
				}
		}finally {
			if(baos!=null) {
				baos.close();
			}
		}
		TwoTuple<byte[], PackageInfo> result = new TwoTuple<byte[], PackageInfo>(baos.toByteArray(), pi);
	    return result;
    }
	private byte[] readByteArray(ZipInputStream zipInputStream) throws IOException {
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
		    byte[] buffer = new byte[1024];
		    int len = -1;
		    while ((len = zipInputStream.read(buffer)) != -1) {
		    	bos.write(buffer, 0, len);
		    }
	
		    return bos.toByteArray();
		}
    }
	@Override
	@Deprecated
	public void uploadContentVersion(UploadOneContent content, String userId) {
		
		String tmp_prefix = "nio_";
		try {
			Path tmp_dir = Files.createTempDirectory(tmp_prefix);
			Path tempOriginalZipPath = Files.createTempFile(tmp_dir, tmp_prefix, "old_package.zip");
			Path tempNewZipPath = Files.createTempFile(tmp_dir, tmp_prefix, "package.zip");
			Path packageFolderDir = Files.createTempDirectory(tmp_dir, tmp_prefix+"package_");
			Path finalNeedPackagePath = Files.createTempFile(tmp_dir, tmp_prefix, "package.cpkg");
			String packageFolderPath = packageFolderDir.toString();
			//Path destFile 
			String finalNeedPackage = finalNeedPackagePath.toString();
//			PackageInfo pi = contentPackageService.unpack(content.getFile(), tempOriginalZipPath, packageFolderPath);
			PackageInfo pi = null;
			if(pi.getDescription()==null || pi.getDescription() == "") {
				pi.setDescription(content.getDescription()); 
			}
			
			//设置版本信息
			setContentPackageInfo(pi, content);
			
			//重置package.json
			byte[] bytes = CommonUtils.convertObjectToJsonBytes(pi);
			String packageJson = packageFolderPath + File.separator + "package.json";
			Path path= Paths.get(packageJson);
			Files.write(path, bytes, StandardOpenOption.TRUNCATE_EXISTING);
			
			//压缩
			contentPackageService.pack(packageFolderPath, tempNewZipPath, pi, finalNeedPackage);
			

			byte[] data = Files.readAllBytes(finalNeedPackagePath);
			addContentVersionToDB(content, userId, pi, data);
			try {
//				FileUtils.forceDelete(tmp_dir.toFile());
			}catch (Exception ex){
				logger.warn(ex.getMessage(),ex);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
			throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_UPLOAD_ERROR);
		}
		
	}

	private void setContentPackageInfo(PackageInfo pi, UploadOneContent content) {
		String softwareVersion = content.getSupportSoftwareVersion();
		short packageType = content.getContentPackageType();
		String contentName = content.getContentName();
		ContentVersion contentVersion = content.getContentVersion();
		setContentPackageInfo(pi, softwareVersion, contentName, packageType, contentVersion);
	}
	

	@Transactional(rollbackOn=Exception.class)
	private void addContentVersionToDB(UploadOneContent content, String userId, PackageInfo pi, byte[] data) {
		Date date = new Date();
		List<MainResourceItem> mainResources = pi.getMains();
		List<ResourceItem> dependentResources = pi.getResources();
		if(mainResources==null) {
			mainResources = Collections.emptyList();
		}
		if(dependentResources == null) {
			dependentResources = Collections.emptyList();
		}
		int resourceCount = mainResources.size() + dependentResources.size();
		//set content package
		ContentPackageEntity cp = new ContentPackageEntity();
		cp.setCreateTime(date);
		cp.setCreateUserId(userId);
		cp.setData(data);
		cp.setResourceCount(resourceCount);
		
		cp = contentPackageRepository.save(cp);
		
		String packageId = cp.getId();
		
		//set content version
		ContentVersionEntity cv = new ContentVersionEntity();
		cv.setName(content.getContentName());
		cv.setCategory(CONTENTVERSIONCATEGORY);
		cv.setType(content.getContentPackageType());
		cv.setSoftwareVersion(content.getSupportSoftwareVersion());
		ContentVersion contentVersion = content.getContentVersion();
		cv.setMajorVersion(contentVersion.getMajor());
		cv.setMinorVersion(contentVersion.getMinor());
		cv.setRevisionVersion(contentVersion.getRevision());
		cv.setContentVersion(ContentVersionConvert.convertContentVersion(
				contentVersion.getMajor(), 
				contentVersion.getMinor(), 
				contentVersion.getRevision()));
		cv.setPublishStatus(CHECKING);
		cv.setActivateStatus(ACTIVATED);
		cv.setDescription(content.getDescription());
		cv.setAuthor(pi.getExportUser());
		cv.setPackageId(packageId);
		cv.setCreateTime(date);
		cv.setCreateUserId(userId);
		cv.setUpdateTime(date);
		cv.setUpdateUserId(userId);
		cv = contentVersionRepository.save(cv);
		
		List<ResourceEntity> resources = new ArrayList<ResourceEntity>();
		//set resources
		for(MainResourceItem item : mainResources) {
			ResourceEntity r = new ResourceEntity();
			r.setPackageId(packageId);
			r.setKey(item.getKey());
			r.setType(item.getType());
			r.setName(item.getName());
			r.setSpace(item.getSpace());
			r.setLocation(item.getLocation());
			//md5 TODO
			r.setMain(true);
			resources.add(r);
		}
		//set dependency resources
		for(ResourceItem item : dependentResources) {
			ResourceEntity r = new ResourceEntity();
			r.setPackageId(packageId);
			r.setKey(item.getKey());
			r.setType(item.getType());
			r.setName(item.getName());
			r.setSpace(item.getSpace());
			r.setLocation(item.getLocation());
			//md5 TODO
			r.setMain(false);
			resources.add(r);
		}
		resourceRepository.saveAll(resources);
	}
	
	/**
	 * 设置 Json 版本信息
	 * @param pi
	 * @param softwareVersion
	 * 
	 * @param packageType
	 * @param contentVersion
	 */
	private void setContentPackageInfo(PackageInfo pi, String softwareVersion,String contentName, short packageType,
			ContentVersion contentVersion) {
		int[] forVersions = this.getLastContentVersionOfSpacialPkg(softwareVersion, contentVersion.getMajor(), contentVersion.getMinor());
		long longVersion = ContentVersionConvert.convertContentVersion(contentVersion.getMajor(), contentVersion.getMinor(), contentVersion.getRevision());
		long longForVersion = ContentVersionConvert.convertContentVersion(forVersions[0],forVersions[1],forVersions[2]);
		if(longForVersion >= longVersion || (packageType != ContentVersionConvert.CONTENTVERSIONWHOLEDTYPE && contentVersion.getRevision() ==ContentVersionConvert.WHOLEDREVISION) 
				|| (packageType == ContentVersionConvert.CONTENTVERSIONWHOLEDTYPE && contentVersion.getRevision() !=ContentVersionConvert.WHOLEDREVISION)) {
			throw new BusinessException(ErrorCodes.ERROR_CONTENT_VERSION_INVALID);
		}
		ContentVersion forContentVersion = new ContentVersion();
		forContentVersion.setMajor(forVersions[0]);
		forContentVersion.setMinor(forVersions[1]);
		forContentVersion.setRevision(forVersions[2]);
		ContentPackageVersionInfo packageVersionInfo = new ContentPackageVersionInfo();
		packageVersionInfo.setForSoftwareVersion(softwareVersion);
		packageVersionInfo.setForContentVersion(forContentVersion);
		packageVersionInfo.setContentPackageVersion(contentVersion);
		List<ContentPackageVersionInfo> packageVersionInfos = new ArrayList<ContentPackageVersionInfo>();
		packageVersionInfos.add(packageVersionInfo);
		if(pi.getContentPackageInfo()==null) {
			ContentPackageInfo cpi = new ContentPackageInfo();
			cpi.setCompatibleSoftwareVersions(new String[] {softwareVersion});
			cpi.setContentPackageName(contentName);
			cpi.setContentPackageType(packageType);
			pi.setContentPackageInfo(cpi);
		}
		pi.getContentPackageInfo().setContentPackageVersions(packageVersionInfos);
	}
	/**
	 * 0:major, 1:minor, 2:revision
	 */
	@Override
	public int[] getMaxContentVersion(String softwareVersion) {
		Optional<Long> maxVersion = contentVersionRepository.getMaxContentVersion(softwareVersion);
		if(maxVersion.isPresent()) {
			int[] versions = ContentVersionConvert.contentVersionFromLongToSplit(maxVersion.get());
			return versions;
		}else {
			return new int[] {0,0,0};
		}
	}
	
	/**
	 * 0:major, 1:minor, 2:0
	 */
	@Override
	public int[] getMaxContentMinorVersion(String softwareVersion) {
		Optional<Long> maxVersion = contentVersionRepository.getMaxContentMinorVersion(softwareVersion);
		if(maxVersion.isPresent()) {
			int[] versions = ContentVersionConvert.contentVersionFromLongToSplit(maxVersion.get());
			return versions;
		}else {
			return new int[] {0,0,0};
		}
	}
	/**
	 * 0:major, 1:minor, 2:revision
	 */
	@Override
	public int[] getLastContentVersionOfSpacialPkg(String softwareVersion, int majorVersion, int minorVersion) {
		Optional<Long> lastVersion = contentVersionRepository.getLastContentVersion(softwareVersion, majorVersion, minorVersion);
		if(lastVersion.isPresent()) {
			int[] versions = ContentVersionConvert.contentVersionFromLongToSplit(lastVersion.get());
			return new int[] {versions[0], versions[1], versions[2]};
		}else {
			return new int[] {0, 0, 0};
		}
	}
	@Override
	public boolean existMinorContentVersion(String softwareVersion, long contentVersion) {
		String pkgId = contentVersionRepository.getContentPackageIdByVersion(softwareVersion, contentVersion);
		return pkgId != null && !pkgId.isEmpty();
	}

	@Override
	public List<ContentVersionEntity> getAllContentVersionList() {
		List<Object[]> lists = contentVersionRepository.getAllContentVersion();
		return toContentVersionEntity(lists);
	}

	private List<ContentVersionEntity> toContentVersionEntity(List<Object[]> lists) {
		List<ContentVersionEntity> result = new ArrayList<ContentVersionEntity>();
		for(Object[] list : lists) {
			int cnt = list.length;
			ContentVersionEntity cv = (ContentVersionEntity)list[0];
			if(cnt >=2 ) {
				cv.setResourceCount((int)list[1]);
			}
			if(cnt >=3 ) {
				cv.setCreateUserName((String)list[2]);
			}
			result.add(cv);
		}
		return result;
	}
	
	@Transactional(rollbackOn=Exception.class)
	@Override
	public int publishContentVersion(String[] ids, String userId) {
		Date date = new Date();
		return contentVersionRepository.updatePublishStatus(ids, PUBLISHED, userId, date);
	}
	
	@Transactional(rollbackOn=Exception.class)
	@Override
	public int disableContentVersion(String[] ids, String userId) {
		Date date = new Date();
		return contentVersionRepository.updateActivateStatus(ids, DISABLED, userId, date);
	}
	
	@Transactional(rollbackOn=Exception.class)
	@Override
	public int updateContentVersionNameAndDesc(String id, String name, String description, String userId) {
		Date date = new Date();
		int result = contentVersionRepository.updateContentVersionNameAndDesc(id, name, description, userId, date);
		return result;
	}

	
}
