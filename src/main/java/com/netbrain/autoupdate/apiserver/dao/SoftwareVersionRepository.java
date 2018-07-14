package com.netbrain.autoupdate.apiserver.dao;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.netbrain.autoupdate.apiserver.model.SoftwareVersionEntity;

public interface SoftwareVersionRepository extends JpaRepository<SoftwareVersionEntity, String>, JpaSpecificationExecutor<SoftwareVersionEntity>, Serializable {


}
