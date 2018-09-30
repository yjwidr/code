package com.xxx.autoupdate.apiserver.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "t_ie_users_info", uniqueConstraints = { @UniqueConstraint(columnNames = { "licenseId" }) })
public class IEUserInfoEntity implements Serializable {
	private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator="idGenerator")
    @GenericGenerator(name="idGenerator", strategy="uuid")
    private String id; 
    /*
     * licenseId， 每个license id 对应一条记录。
     */
    @Column(nullable=false,length=128)
    private String licenseId;
    /*
     * IE User name
     */
    @Column(length=128)
    private String userName;
    @Column(length=128)
    private String email;
    @Column(length=1024)
    private String company;
    @Column(length=5120)
    private String description;
    @Column(length=32)
    private String ieIp;
    @Column(length=128)
    private String softwareVersion;
    /*
     * package版本号,完整版本号，3+3+6
     */
    private long contentVersion;
    private short contentMajorVersion;
    private short contentMinorVersion;
    private short contentRevisionVersion;
    /*
     * 最后探测时间
     */
    @Column(columnDefinition = "timestamp with time zone")
    private Date lastDetectTime;
    
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLicenseId() {
		return licenseId;
	}
	public void setLicenseId(String licenseId) {
		this.licenseId = licenseId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getCompany() {
		return company;
	}
	public void setCompany(String company) {
		this.company = company;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getIeIp() {
		return ieIp;
	}
	public void setIeIp(String ieIp) {
		this.ieIp = ieIp;
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
	public short getContentMajorVersion() {
		return contentMajorVersion;
	}
	public void setContentMajorVersion(short contentMajorVersion) {
		this.contentMajorVersion = contentMajorVersion;
	}
	public short getContentMinorVersion() {
		return contentMinorVersion;
	}
	public void setContentMinorVersion(short contentMinorVersion) {
		this.contentMinorVersion = contentMinorVersion;
	}
	public short getContentRevisionVersion() {
		return contentRevisionVersion;
	}
	public void setContentRevisionVersion(short contentRevisionVersion) {
		this.contentRevisionVersion = contentRevisionVersion;
	}
	public Date getLastDetectTime() {
		return lastDetectTime;
	}
	public void setLastDetectTime(Date lastDetectTime) {
		this.lastDetectTime = lastDetectTime;
	}
}
