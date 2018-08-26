package com.xxx.autoupdate.apiserver.dao;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.xxx.autoupdate.apiserver.model.ContentVersionEntity;

public interface ContentVersionRepository extends JpaRepository<ContentVersionEntity, String>, JpaSpecificationExecutor<ContentVersionEntity>, Serializable {
	
	/**
	 * 不包含content version,探测用
	 * @param softwareVersion
	 * @param contentVersion >?
	 * @return
	 */
	@Query(value = "select cv, cp.resourceCount, u.userName from ContentVersionEntity cv join ContentPackageEntity cp on cv.packageId = cp.id join UserEntity u on cv.createUserId = u.id where cv.softwareVersion = ?1 and cv.contentVersion > ?2 order by cv.contentVersion desc")
	List<Object[]> getGTBySoftVersionIdAndContentVersion(String softwareVersion, long contentVersion);
	
	/**
	 * 包含content version，内部使用，不需要resourceCount和createUserName
	 * @param softwareVersion
	 * @param contentVersion >=?
	 * @return
	 */
	@Query(value = "select cv, cp.resourceCount from ContentVersionEntity cv join ContentPackageEntity cp on cv.packageId = cp.id where cv.softwareVersion = ?1 and cv.contentVersion >= ?2 order by cv.contentVersion desc")
	List<Object[]> getGTEBySoftVersionIdAndContentVersion(String softwareVersion, long contentVersion);
	
	@Query(value = "select cv.packageId from ContentVersionEntity cv where cv.softwareVersion = ?1 and cv.contentVersion = ?2")
	String getContentPackageIdByVersion(String softwareVersion, long contentVersion);
	
	
	@Query(value = "select max(cv.contentVersion) from ContentVersionEntity cv where cv.softwareVersion = ?1")
	Optional<Long> getMaxContentVersion(String softwareVersion);
	
	@Query(value = "select max(cv.contentVersion) from ContentVersionEntity cv where cv.softwareVersion = ?1 and cv.majorVersion = ?2 and cv.minorVersion = ?3")
	Optional<Long> getLastContentVersion(String softwareVersion, int majorVersion, int minorVersion);
	
	@Query(value = "select max(cv.contentVersion) from ContentVersionEntity cv where cv.softwareVersion = ?1 and cv.revisionVersion = 0")
	Optional<Long> getMaxContentMinorVersion(String softwareVersion);
	
	@Modifying(clearAutomatically = true)
	@Query(value = "update ContentVersionEntity cv set cv.publishStatus = ?2, cv.updateUserId = ?3, cv.updateTime = ?4 where cv.id in (?1)")
	int updatePublishStatus(String[] ids, short status, String userId, Date date);
	
	@Modifying(clearAutomatically = true)
	@Query(value = "update ContentVersionEntity cv set cv.activateStatus = ?2, cv.updateUserId = ?3, cv.updateTime = ?4 where cv.id in (?1)")
	int updateActivateStatus(String[] ids, short status, String userId, Date date);
	
	@Modifying(clearAutomatically = true)
	@Query(value = "update ContentVersionEntity cv set cv.name = ?2, cv.description = ?3, cv.updateUserId = ?4, cv.updateTime = ?5 where cv.id = ?1")
	int updateContentVersionNameAndDesc(String id, String name, String description, String userId, Date date);
	
	/**
	 * 不包含已经删除的
	 * @return
	 */
	@Query(value = "select cv, cp.resourceCount, u.userName from ContentVersionEntity cv join ContentPackageEntity cp on cv.packageId = cp.id join UserEntity u on cv.createUserId = u.id where cv.activateStatus != 3 order by cv.contentVersion desc")
	List<Object[]> getAllContentVersion();
}
