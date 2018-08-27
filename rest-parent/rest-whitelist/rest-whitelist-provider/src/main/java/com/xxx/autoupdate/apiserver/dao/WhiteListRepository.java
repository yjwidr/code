package com.xxx.autoupdate.apiserver.dao;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.xxx.autoupdate.apiserver.model.WhiteListEntity;

public interface WhiteListRepository extends JpaRepository<WhiteListEntity, String>, JpaSpecificationExecutor<WhiteListEntity>, Serializable {
	@Query(value = "from WhiteListEntity wl where wl.contentVersionId = ?1")
	List<WhiteListEntity> getWhiteListByContentVersionId(String contentVersionId);
	
	@Query(value = "select wl.licenseId from WhiteListEntity wl where wl.contentVersionId = ?1 and licenseId = ?2")
	String getWhiteListByContentVersionId(String contentVersionId, String licenseId);
	
}
