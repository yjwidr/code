package com.netbrain.autoupdate.apiserver.dao;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.netbrain.autoupdate.apiserver.model.IEUserInfoEntity;

public interface IEUserInfoRepository extends JpaRepository<IEUserInfoEntity, String>, JpaSpecificationExecutor<IEUserInfoEntity>, Serializable {
	@Query(value = "from IEUserInfoEntity u where u.licenseId in (?1)")
	List<IEUserInfoEntity> getByLicenseIds(String[] licenseId);
	
	@Query(value = "from IEUserInfoEntity u where u.licenseId = ?1")
	IEUserInfoEntity getByLicenseId(String licenseId);
}
