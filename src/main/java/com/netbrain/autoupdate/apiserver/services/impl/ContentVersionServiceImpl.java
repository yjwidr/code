package com.netbrain.autoupdate.apiserver.services.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.netbrain.autoupdate.apiserver.dao.ContentPackageRepository;
import com.netbrain.autoupdate.apiserver.dao.ContentVersionRepository;
import com.netbrain.autoupdate.apiserver.dao.WhiteListRepository;
import com.netbrain.autoupdate.apiserver.exception.BusinessException;
import com.netbrain.autoupdate.apiserver.exception.ErrorCodes;
import com.netbrain.autoupdate.apiserver.model.ContentPackageEntity;
import com.netbrain.autoupdate.apiserver.model.ContentVersionEntity;
import com.netbrain.autoupdate.apiserver.model.parameter.UploadContent;
import com.netbrain.autoupdate.apiserver.model.parameter.UploadOneContent;
import com.netbrain.autoupdate.apiserver.services.ContentVersionService;
import com.netbrain.autoupdate.apiserver.util.CommonUtils;
import com.netbrain.autoupdate.apiserver.util.ContentVersionConvert;
import com.netbrain.autoupdate.apiserver.util.TwoTuple;
@Service
public class ContentVersionServiceImpl implements ContentVersionService {
	
	@Autowired
	private ContentVersionRepository contentVersionRepository;
	
	@Autowired
	private WhiteListRepository whiteListRepository;
	
	@Autowired
	private ContentPackageRepository contentPackageRepository;
	
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
	 * @see com.netbrain.autoupdate.apiserver.services.ContentVersionService#getDetectVersions(java.lang.String, long, String)
	 */
	@Override
	public List<ContentVersionEntity> getDetectVersions(String softwareVersion, String contentVersion, String licenseId){
		boolean hasLicense = licenseId != null && licenseId != "";
		long contentVersionLong = ContentVersionConvert.contentVersionFromStrToLong(contentVersion);
		List<ContentVersionEntity> list = contentVersionRepository.getGTBySoftVersionIdAndContentVersion(softwareVersion, contentVersionLong);
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
	 * @see com.netbrain.autoupdate.apiserver.services.ContentVersionService#downloadOnePackage(java.lang.String, java.lang.String)
	 */
	@Override
	public ContentPackageEntity downloadOnePackage(String softwareVersion, String contentVersion) {
		long contentVersionLong = ContentVersionConvert.contentVersionFromStrToLong(contentVersion);
		String packageId = contentVersionRepository.getContentPackageIdByVersion(softwareVersion, contentVersionLong);
		ContentPackageEntity cpEntity = CommonUtils.toObject(contentPackageRepository.findById(packageId));
		if(cpEntity!=null) {
			cpEntity.setVersion(softwareVersion + "_" + contentVersion);
			return cpEntity;
		}else {
			return null;
		}
	}

	/*
	 * 下载多个版本
	 * @see com.netbrain.autoupdate.apiserver.services.ContentVersionService#downloadMultiPackage(java.lang.String, java.lang.String[])
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
			List<ContentVersionEntity> list = contentVersionRepository.getGTEBySoftVersionIdAndContentVersion(softwareVersion, from);
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
				
				cp.setVersion(softwareVersion+"_"+ContentVersionConvert.contentVersionFromLongToStr(packageVersion.get(cp.getId())));
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
					throw new BusinessException(ErrorCodes.ERROR_CONTENTVERSION_ERROR);
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
	 * @see com.netbrain.autoupdate.apiserver.services.ContentVersionService#uploadContentVersions(com.netbrain.autoupdate.apiserver.model.parameter.UploadContent)
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
			long maxConventVersion = contentVersionRepository.getMaxContentVersion(softwareVersion);
			int[] versions = ContentVersionConvert.addContentVersionForSplit(maxConventVersion, content.getContentPackageType());
			cv.setMajorVersion(versions[0]);
			cv.setMinorVersion(versions[1]);
			cv.setRevisionVersion(versions[2]);
			cv.setContentVersion(ContentVersionConvert.convertContentVersion(versions[0], versions[1], versions[2]));
		}
		//TODO
		return false;
	}
	@Transactional(rollbackOn=Exception.class)
	@Override
	public ContentVersionEntity uploadContentVersion(UploadOneContent content, String packageId, String userId, String author) {
		ContentVersionEntity cv = new ContentVersionEntity();
		cv.setName(content.getContentName());
		cv.setCategory(CONTENTVERSIONCATEGORY);
		cv.setType(content.getContentPackageType());
		String softwareVersion = content.getSupportSoftwareVersion();
		cv.setSoftwareVersion(softwareVersion);
		cv.setMajorVersion(content.getContentVersion().getMajor());
		cv.setMinorVersion(content.getContentVersion().getMinor());
		cv.setRevisionVersion(content.getContentVersion().getRevision());
		cv.setContentVersion(ContentVersionConvert.convertContentVersion(
				content.getContentVersion().getMajor(), 
				content.getContentVersion().getMinor(), 
				content.getContentVersion().getRevision()));
		cv.setPublishStatus(CHECKING);
		cv.setActivateStatus(ACTIVATED);
		cv.setDescription(content.getDescription());
		cv.setAuthor(author);
		cv.setPackageId(packageId);
		Date date = new Date();
		cv.setCreateTime(date);
		cv.setCreateUserId(userId);
		cv.setUpdateTime(date);
		cv.setCreateUserId(userId);
		cv = contentVersionRepository.save(cv);
		return cv;
	}
	/*
	 * 0:major, 1:minor, 2:revision
	 * @see com.netbrain.autoupdate.apiserver.services.ContentVersionService#getMaxContentVersion(java.lang.String)
	 */
	@Override
	public int[] getMaxContentVersion(String softwareVersion) {
		long maxVersion = contentVersionRepository.getMaxContentVersion(softwareVersion);
		int[] versions = ContentVersionConvert.contentVersionFromLongToSplit(maxVersion);
		return versions;
	}
	
	/*
	 * 0:major, 1:minor
	 * @see com.netbrain.autoupdate.apiserver.services.ContentVersionService#getMaxContentMinorVersion(java.lang.String)
	 */
	@Override
	public int[] getMaxContentMinorVersion(String softwareVersion) {
		long maxVersion = contentVersionRepository.getMaxContentMinorVersion(softwareVersion);
		int[] versions = ContentVersionConvert.contentVersionFromLongToSplit(maxVersion);
		return new int[] {versions[0], versions[1]};
	}

	@Override
	public List<ContentVersionEntity> getAllContentVersionList() {
		Sort sort = new Sort(Sort.Direction.DESC, "contentVersion");
		List<ContentVersionEntity> lists = contentVersionRepository.findAll(sort);
		return lists;
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
	
}
