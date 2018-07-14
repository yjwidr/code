package com.netbrain.autoupdate.apiserver.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonFilter;

/*
 * Software Version指IE软件版本，SoftwareVersion表存储IE软件版本信息，
 * 主要用于提交ContentVersion时关联其所依赖的SoftwareVersion，
 * 以便作为Content可升级性的检测判断依据之一。
 */
@Entity
@Table(name = "t_software_versions", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
@JsonFilter("com.netbrain.autoupdate.apiserver.model.SoftwareVersionEntity")
public class SoftwareVersionEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@Id
    @Column(length=32,nullable=false)
    private String version; 
    @Column(nullable=false,length=32)
    private String name;
    @Column(length=5120)
    private String description;
    
    @CreatedDate
    @Column(columnDefinition = "timestamp with time zone not null DEFAULT (now())",nullable=false)
    private Date createTime;
    @LastModifiedDate
    @Column(columnDefinition = "timestamp with time zone not null DEFAULT (now())",nullable=false)
    private Date updateTime;
    @Column(nullable=false)
    private String createUserId;
    @Column(nullable=false)
    private String updateUserId;
    
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
    public String getName() {
    	return name; 
    }
    public void setName(String name) {
    	this.name = name;
    }
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public String getCreateUserId() {
		return createUserId;
	}
	public void setCreateUserId(String createUserId) {
		this.createUserId = createUserId;
	}
	public Date getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	public String getUpdateUserId() {
		return updateUserId;
	}
	public void setUpdateUserId(String updateUserId) {
		this.updateUserId = updateUserId;
	}
}
