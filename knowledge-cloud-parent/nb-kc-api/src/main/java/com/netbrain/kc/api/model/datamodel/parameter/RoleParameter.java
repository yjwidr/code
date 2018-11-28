package com.netbrain.kc.api.model.datamodel.parameter;

import javax.validation.constraints.NotBlank;

public class RoleParameter  {
	private String id;
	@NotBlank(message = "name cannot be empty")
    private String name;
    private String description;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

}
