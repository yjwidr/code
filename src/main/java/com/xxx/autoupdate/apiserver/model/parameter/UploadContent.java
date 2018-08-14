package com.xxx.autoupdate.apiserver.model.parameter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;
import org.springframework.web.multipart.MultipartFile;

public class UploadContent {
    @NotBlank(message ="contentName cannot be empty")
    @Length(min=1, max=128, message="contentName length must be between 1-128")
    @Pattern(regexp = "^[A-Za-z]+[\\s\\S]*+$", message = "the contentName must begin with an alphabetic or underscore")
    private String contentName;
    @NotNull(message ="contentPackageType cannot be empty")
    @Range(min=0,max=9,message="contentPackageType must be between 1-9")
    private short contentPackageType;
    @Size(min= 1,message ="supportSoftwareVersions cannot be empty")
    @NotEmpty(message ="supportSoftwareVersions cannot be empty")
    private String[] supportSoftwareVersions;
    private String description;
    private MultipartFile file;

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

    public String[] getSupportSoftwareVersions() {
        return supportSoftwareVersions;
    }

    public void setSupportSoftwareVersions(String[] supportSoftwareVersions) {
        this.supportSoftwareVersions = supportSoftwareVersions;
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
