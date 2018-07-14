package com.netbrain.autoupdate.apiserver.dao;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.netbrain.autoupdate.apiserver.model.ContentVersionEntity;

public interface ContentVersionRepository extends JpaRepository<ContentVersionEntity, String>, JpaSpecificationExecutor<ContentVersionEntity>, Serializable {
	
	/*
	 * 不包含content version
	 */
	@Query(value = "from ContentVersionEntity cv where cv.softwareVersion = ?1 and cv.contentVersion > ?2 order by cv.contentVersion desc")
	List<ContentVersionEntity> getGTBySoftVersionIdAndContentVersion(String softwareVersion, long contentVersion);
	
	/*
	 * 包含content version
	 */
	@Query(value = "from ContentVersionEntity cv where cv.softwareVersion = ?1 and cv.contentVersion >= ?2 order by cv.contentVersion desc")
	List<ContentVersionEntity> getGTEBySoftVersionIdAndContentVersion(String softwareVersion, long contentVersion);
	
	@Query(value = "select cv.packageId from ContentVersionEntity cv where cv.softwareVersion = ?1 and cv.contentVersion = ?2")
	String getContentPackageIdByVersion(String softwareVersion, long contentVersion);
	
	@Query(value = "select max(cv.contentVersion) from ContentVersionEntity cv where cv.softwareVersion = ?1")
	long getMaxContentVersion(String softwareVersion);
	
	@Query(value = "select max(cv.contentVersion) from ContentVersionEntity cv where cv.softwareVersion = ?1 and revisionVersion = 0")
	long getMaxContentMinorVersion(String softwareVersion);
	
	@Modifying(clearAutomatically = true)
	@Query(value = "update ContentVersionEntity cv set cv.publishStatus = ?2, cv.updateUserId = ?3, cv.updateTime = ?4 where cv.id in (?1)")
	int updatePublishStatus(String[] ids, short status, String userId, Date date);
	
	@Modifying(clearAutomatically = true)
	@Query(value = "update ContentVersionEntity cv set cv.activateStatus = ?2, cv.updateUserId = ?3, cv.updateTime = ?4 where cv.id in (?1)")
	int updateActivateStatus(String[] ids, short status, String userId, Date date);
}
