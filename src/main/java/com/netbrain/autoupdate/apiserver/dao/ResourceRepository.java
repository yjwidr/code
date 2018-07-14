package com.netbrain.autoupdate.apiserver.dao;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.netbrain.autoupdate.apiserver.model.ResourceEntity;

public interface ResourceRepository extends JpaRepository<ResourceEntity, String>, JpaSpecificationExecutor<ResourceEntity>, Serializable {

}
