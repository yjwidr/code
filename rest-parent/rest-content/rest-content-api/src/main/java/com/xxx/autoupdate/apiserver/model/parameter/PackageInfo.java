package com.xxx.autoupdate.apiserver.model.parameter;

import java.io.Serializable;
import java.util.List;

public class PackageInfo extends PackageSummary implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<ResourceItem> resources;

	public List<ResourceItem> getResources() {
		return resources;
	}

	public void setResources(List<ResourceItem> resources) {
		this.resources = resources;
	}
}
