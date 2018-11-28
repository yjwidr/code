package com.netbrain.kc.api.provider.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.netbrain.kc.api.model.datamodel.PermissionEntity;  
  
public interface PermissionRepository extends JpaRepository<PermissionEntity, String>,JpaSpecificationExecutor<PermissionEntity>,Serializable{  
  
} 