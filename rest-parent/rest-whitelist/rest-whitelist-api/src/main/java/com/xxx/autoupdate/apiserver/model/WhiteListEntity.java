package com.xxx.autoupdate.apiserver.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "t_white_list")
public class WhiteListEntity implements Serializable {
	private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator="idGenerator")
    @GenericGenerator(name="idGenerator", strategy="uuid")
    private String id; 
    @Column(nullable=false)
    private String contentVersionId;
    @Column(nullable=false)
    private String licenseId;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getContentVersionId() {
		return contentVersionId;
	}
	public void setContentVersionId(String contentVersionId) {
		this.contentVersionId = contentVersionId;
	}
	public String getLicenseId() {
		return licenseId;
	}
	public void setLicenseId(String licenseId) {
		this.licenseId = licenseId;
	}
}
