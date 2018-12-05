package com.xxx.autoupdate.apiserver.dao;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.xxx.autoupdate.apiserver.model.PermissionEntity;  
  
public interface PermissionRepository extends JpaRepository<PermissionEntity, String>,JpaSpecificationExecutor<PermissionEntity>,Serializable{  
  
} 