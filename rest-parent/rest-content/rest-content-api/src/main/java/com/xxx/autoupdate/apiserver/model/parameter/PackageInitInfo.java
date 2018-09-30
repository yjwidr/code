package com.xxx.autoupdate.apiserver.model.parameter;

public class PackageInitInfo {
	private int packageVersion;
	private int summaryDataLength;
	private int listDataLength;
	private String base64Data;
	public int getPackageVersion() {
		return packageVersion;
	}
	public void setPackageVersion(int packageVersion) {
		this.packageVersion = packageVersion;
	}
	public int getSummaryDataLength() {
		return summaryDataLength;
	}
	public void setSummaryDataLength(int summaryDataLength) {
		this.summaryDataLength = summaryDataLength;
	}
	public int getListDataLength() {
		return listDataLength;
	}
	public void setListDataLength(int listDataLength) {
		this.listDataLength = listDataLength;
	}
	public String getBase64Data() {
		return base64Data;
	}
	public void setBase64Data(String base64Data) {
		this.base64Data = base64Data;
	}
}
