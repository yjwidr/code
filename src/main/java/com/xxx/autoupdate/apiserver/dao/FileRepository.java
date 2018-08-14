package com.xxx.autoupdate.apiserver.dao;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.xxx.autoupdate.apiserver.model.FileEntity;  
  
public interface FileRepository extends JpaRepository<FileEntity, String>,JpaSpecificationExecutor<FileEntity>,Serializable{  
  
} 