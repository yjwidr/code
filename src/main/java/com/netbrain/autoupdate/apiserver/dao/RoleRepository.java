package com.netbrain.autoupdate.apiserver.dao;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.netbrain.autoupdate.apiserver.model.RoleEntity;  
  
public interface RoleRepository extends JpaRepository<RoleEntity, String>,JpaSpecificationExecutor<RoleEntity>,Serializable{  
  
} 