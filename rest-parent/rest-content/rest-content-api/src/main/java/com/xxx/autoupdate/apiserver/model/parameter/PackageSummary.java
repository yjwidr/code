package com.xxx.autoupdate.apiserver.model.parameter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.Transient;

import com.alibaba.fastjson.annotation.JSONField;

public class PackageSummary implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int packageVersion;
	private String exportUser;
	@JSONField(format="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")  
	private Date exportDate;
	private String packageFileName;
	@Transient
	private Optional<Integer> zipDataSize;
	/*
	 * 导出包的IE版本
	 */
	private String exportOnSoftwareVersion;
	
	private ContentPackageInfo contentPackageInfo;
	/*
	 * 主要资源集合(导出时针对的资源，其他资源为连带导出的资源)
	 */
	private List<MainResourceItem> mains;
	/*
	 * //包含的file zip 列表
	 */
	private List<ServiceFileZipInfo> serviceFileZips;
	/*
	 * new
	 */
	private String description;
	
	private String dataMd5;
	
	public void loadFrom(PackageSummary source) {
		this.setPackageVersion(source.getPackageVersion());
		this.setPackageFileName(source.getPackageFileName());
		this.setExportUser(source.getExportUser());
		this.setExportDate(source.getExportDate());
		this.setZipDataSize(source.getZipDataSize());
		this.setMains(source.getMains());
		this.setExportOnSoftwareVersion(source.getExportOnSoftwareVersion());
		this.setContentPackageInfo(source.getContentPackageInfo());
		this.setServiceFileZips(source.getServiceFileZips());
		this.setDataMd5(source.getDataMd5());
		this.setDescription(source.getDescription());
	}
	
	public int getPackageVersion() {
		return packageVersion;
	}
	public void setPackageVersion(int packageVersion) {
		this.packageVersion = packageVersion;
	}
	public String getExportUser() {
		return exportUser;
	}
	public void setExportUser(String exportUser) {
		this.exportUser = exportUser;
	}
	public Date getExportDate() {
		return exportDate;
	}
	public void setExportDate(Date exportDate) {
		this.exportDate = exportDate;
	}
	public String getPackageFileName() {
		return packageFileName;
	}
	public void setPackageFileName(String packageFileName) {
		this.packageFileName = packageFileName;
	}
	public Optional<Integer> getZipDataSize() {
		return zipDataSize;
	}
	public void setZipDataSize(Optional<Integer> zipDataSize) {
		this.zipDataSize = zipDataSize;
	}

	public String getExportOnSoftwareVersion() {
		return exportOnSoftwareVersion;
	}

	public void setExportOnSoftwareVersion(String exportOnSoftwareVersion) {
		this.exportOnSoftwareVersion = exportOnSoftwareVersion;
	}

	public ContentPackageInfo getContentPackageInfo() {
		return contentPackageInfo;
	}

	public void setContentPackageInfo(ContentPackageInfo contentPackageInfo) {
		this.contentPackageInfo = contentPackageInfo;
	}

	public List<MainResourceItem> getMains() {
		return mains;
	}

	public void setMains(List<MainResourceItem> mains) {
		this.mains = mains;
	}

	public List<ServiceFileZipInfo> getServiceFileZips() {
		return serviceFileZips;
	}

	public void setServiceFileZips(List<ServiceFileZipInfo> serviceFileZips) {
		this.serviceFileZips = serviceFileZips;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDataMd5() {
		return dataMd5;
	}

	public void setDataMd5(String dataMd5) {
		this.dataMd5 = dataMd5;
	}
}