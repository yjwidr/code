package com.netbrain.autoupdate.apiserver.model.parameter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;
import org.springframework.web.multipart.MultipartFile;

public class UploadOneContent {
    @NotBlank(message ="contentName cannot be empty")
    @Length(min=1, max=32, message="contentName length must be between 1-32")
    @Pattern(regexp = "^[A-Za-z]+[\\s\\S]*+$", message = "the contentName must begin with an alphabetic or underscore")
    private String contentName;
    @NotNull(message ="contentPackageType cannot be empty")
    @Range(min=0,max=9,message="contentPackageType must be between 1-9")
    private short contentPackageType;
    @Size(min= 1,message ="supportSoftwareVersions cannot be empty")
    @NotEmpty(message ="supportSoftwareVersions cannot be empty")
    private String supportSoftwareVersion;
    private String description;
    private MultipartFile file;
    private ContentVersion contentVersion;

    public ContentVersion getContentVersion() {
		return contentVersion;
	}

	public void setContentVersion(ContentVersion contentVersion) {
		this.contentVersion = contentVersion;
	}

	public String getContentName() {
        return contentName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    public short getContentPackageType() {
        return contentPackageType;
    }

    public void setContentPackageType(short contentPackageType) {
        this.contentPackageType = contentPackageType;
    }

    public String getSupportSoftwareVersion() {
        return supportSoftwareVersion;
    }

    public void setSupportSoftwareVersion(String supportSoftwareVersion) {
        this.supportSoftwareVersion = supportSoftwareVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
