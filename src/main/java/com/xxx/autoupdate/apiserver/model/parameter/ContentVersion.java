package com.xxx.autoupdate.apiserver.model.parameter;

import java.io.Serializable;

public class ContentVersion implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int major;
	private int minor;
	private int revision;
	
	public int getMajor() {
		return major;
	}
	public void setMajor(int major) {
		this.major = major;
	}
	public int getMinor() {
		return minor;
	}
	public void setMinor(int minor) {
		this.minor = minor;
	}
	public int getRevision() {
		return revision;
	}
	public void setRevision(int revision) {
		this.revision = revision;
	}
}
