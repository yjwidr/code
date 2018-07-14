package com.netbrain.autoupdate.apiserver.model.parameter;

import javax.validation.constraints.NotBlank;

public class WhiteListUser {
    
	private String id;
	private String contentVersionId;
	@NotBlank(message ="licenseId cannot be empty")
	private String licenseId;
	@NotBlank(message ="userName cannot be empty")
	private String userName;
	private String email;
	private String company;
	private String description;
	
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
}
