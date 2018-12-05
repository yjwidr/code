package com.xxx.autoupdate.apiserver.model.parameter;

import java.io.Serializable;

import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public class UploadOneContent implements Serializable {
  private static final long serialVersionUID = 1L;
  private String contentName;
  private short contentPackageType;
  private String supportSoftwareVersion;
  private String description;
  @Transient
  private MultipartFile file;
  private byte[] bytes;
  private ContentVersion contentVersion;
  @NotBlank(message ="content info cannot be empty")
  private String info;

  public String getInfo() {
      return info;
  }
  public void setInfo(String info) {
      this.info = info;
  }

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
    public byte[] getBytes() {
        return bytes;
    }
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
