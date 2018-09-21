package com.netbrain.autoupdate.apiagent.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;

public class ContentVersion implements Serializable {
	private static final long serialVersionUID = 1L;
    private String id; 
    private String name;
    private short type;
    private String softwareVersion;
    private transient long contentVersion;
    private int majorVersion;
    private int minorVersion;
    private int resourceCount;
    private int revisionVersion;
    private Map<String,Integer> contentPackageVersion;
    @SuppressWarnings("unused")
	private transient String cv;
    
    public String getCv() {
        return this.majorVersion+"."+this.minorVersion+"."+this.revisionVersion;
    }
    public Map<String,Integer> getContentPackageVersion() {
    	this.contentPackageVersion=new HashMap<>();
		this.contentPackageVersion.put("major", this.majorVersion);
		this.contentPackageVersion.put("minor", this.minorVersion);
		this.contentPackageVersion.put("revision", this.revisionVersion);
    	return this.contentPackageVersion;
    }   
    private short publishStatus;
    private short activateStatus;
    private String description;
    private String author;
    @JSONField(format="yyyy-MM-dd'T'HH:mm:ss.SSSZ")  
    private Date createTime;
    @JSONField(format="yyyy-MM-dd'T'HH:mm:ss.SSSZ")  
    private Date updateTime;

    public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public short getType() {
		return type;
	}
	public void setType(short type) {
		this.type = type;
	}
	public String getSoftwareVersion() {
		return softwareVersion;
	}
	public void setSoftwareVersion(String softwareVersion) {
		this.softwareVersion = softwareVersion;
	}
	public long getContentVersion() {
		return contentVersion;
	}
	public void setContentVersion(long contentVersion) {
		this.contentVersion = contentVersion;
	}
	public int getMajorVersion() {
		return majorVersion;
	}
	public void setMajorVersion(int majorVersion) {
		this.majorVersion = majorVersion;
	}
	public int getMinorVersion() {
		return minorVersion;
	}
	public void setMinorVersion(int minorVersion) {
		this.minorVersion = minorVersion;
	}
	public int getRevisionVersion() {
		return revisionVersion;
	}
	public void setRevisionVersion(int revisionVersion) {
		this.revisionVersion = revisionVersion;
	}
	public short getPublishStatus() {
		return publishStatus;
	}
	public void setPublishStatus(short publishStatus) {
		this.publishStatus = publishStatus;
	}
	public short getActivateStatus() {
		return activateStatus;
	}
	public void setActivateStatus(short activateStatus) {
		this.activateStatus = activateStatus;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public int getResourceCount() {
        return resourceCount;
    }
    public void setResourceCount(int resourceCount) {
        this.resourceCount = resourceCount;
    }
    public Date getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

}

