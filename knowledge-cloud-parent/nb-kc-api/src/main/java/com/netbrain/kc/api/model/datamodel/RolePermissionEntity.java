package com.netbrain.kc.api.model.datamodel;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.netbrain.kc.framework.entity.BaseEntity;

@Entity
@Table(name = "t_role_permissions")
@JsonFilter("com.netbrain.kc.api.model.datamodel.RolePermissionEntity")
public class RolePermissionEntity  extends BaseEntity {

    private static final long serialVersionUID = 1L;
    @NotBlank
    private String roleId;
    @NotBlank
    private String permissionId;
	public String getRoleId() {
		return roleId;
	}
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	public String getPermissionId() {
		return permissionId;
	}
	public void setPermissionId(String permissionId) {
		this.permissionId = permissionId;
	}
}
