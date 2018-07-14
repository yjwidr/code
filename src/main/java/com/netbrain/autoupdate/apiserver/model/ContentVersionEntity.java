package com.netbrain.autoupdate.apiserver.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

/*
 * Content Version指Content  Package的版本，
 * ContentVersion表存储Content Package版本信息，每个版本号值唯一，
 * 通过版本号排序形成版本链，版本号越大版本越高。
 * ContentVersion 包含一个状态信息，体现版本包的状态，为可见性与相关权限提供判断依据
 */
@Entity
@Table(name = "t_content_versions", uniqueConstraints= {
		@UniqueConstraint(columnNames={"category","softwareVersion","contentVersion"}),
		@UniqueConstraint(columnNames={"name"})
})
public class ContentVersionEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@Id
    @GeneratedValue(generator="idGenerator")
    @GenericGenerator(name="idGenerator", strategy="uuid")
    private String id; 
    @Column(nullable=false, length=32)
    private String name;
    /*
     * 包种类，预留，未来可能通过此字段将file resource和db resource分开，当前固定填1
     */
    @Column(nullable=false)
    private short category;
    /*
     * 包类型，1：全量包，2：补丁包
     */
    @Column(nullable=false)
    private short type;
    @Column(length=32, nullable=false)
    private String softwareVersion;
	/*
	 * package版本号,完整版本号，3+3+6
	 */
    @Column(nullable=false)
    private long contentVersion;
    @Column(nullable=false)
    private int majorVersion;
    @Column(nullable=false)
    private int minorVersion;
    /*
     * Package修正版本号(补丁)，全量包时填0
     */
    @Column(nullable=false)
    private int revisionVersion;
    
    @Transient
    private Map<String,Integer> contentPackageVersion;
    
    public Map<String,Integer> getContentPackageVersion() {
    	this.contentPackageVersion=new HashMap<>();
		this.contentPackageVersion.put("major", this.majorVersion);
		this.contentPackageVersion.put("minor", this.minorVersion);
		this.contentPackageVersion.put("revision", this.revisionVersion);
    	return this.contentPackageVersion;
    }   

	/*
     * 发布状态，1：checking，2：published
     */
    @Column(nullable=false)
    private short publishStatus;
    /*
     * 激活状态，1：normal，2：disabled，3：deleted
     */
    @Column(nullable=false)
    private short activateStatus;
    @Column(length=5120)
    private String description;
    @Column(nullable=false, length=32)
    private String author;
    @Column(nullable=false)
    private String packageId;

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
	public short getCategory() {
		return category;
	}
	public void setCategory(short category) {
		this.category = category;
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
	public String getPackageId() {
		return packageId;
	}
	public void setPackageId(String packageId) {
		this.packageId = packageId;
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

