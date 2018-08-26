package com.xxx.autoupdate.apiserver.dao;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.xxx.autoupdate.apiserver.model.ResourceEntity;

public interface ResourceRepository extends JpaRepository<ResourceEntity, String>, JpaSpecificationExecutor<ResourceEntity>, Serializable {
	@Query(value = "from ResourceEntity r where r.packageId = ?1")
	List<ResourceEntity> getResourcesByPackageId(String packageId);
}
