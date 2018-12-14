package com.netbrain.kc.api.model.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.netbrain.kc.framework.entity.BaseEntity;

@Entity
@Table(name = "t_permissions",uniqueConstraints = { @UniqueConstraint(columnNames = {"name"})})
@JsonFilter("com.netbrain.kc.api.model.datamodel.PermissionEntity")
public class PermissionEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @NotBlank(message ="name cannot be empty")
    @Length(max=128)
    @Column(nullable=false,length=128)
    private String name;
	@Column(length=128)
    private String description;

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
