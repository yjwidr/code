package com.xxx.autoupdate.apiserver.model.parameter;

import java.util.List;

public class MainResourceItem extends ResourceItem {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<ResourceCount> counts;

	public List<ResourceCount> getCounts() {
		return counts;
	}

	public void setCounts(List<ResourceCount> counts) {
		this.counts = counts;
	}
}
