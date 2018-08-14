package com.xxx.autoupdate.apiserver.dao;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.xxx.autoupdate.apiserver.model.RoleEntity;  
  
public interface RoleRepository extends JpaRepository<RoleEntity, String>,JpaSpecificationExecutor<RoleEntity>,Serializable{  
  
} 