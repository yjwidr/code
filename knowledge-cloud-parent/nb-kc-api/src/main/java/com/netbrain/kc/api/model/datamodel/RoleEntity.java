package com.netbrain.kc.api.model.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.netbrain.kc.framework.entity.BaseEntity;

@Entity
@Table(name = "t_roles", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
@JsonFilter("com.netbrain.kc.api.model.datamodel.RoleEntity")
public class RoleEntity extends BaseEntity  {
	
	private static final long serialVersionUID = 2L;

	@NotBlank(message = "name cannot be empty")
    @Length(max = 128)
    @Column(nullable=false,length=128)
    private String name;
    @Column(length=64)
    private String description;
    @Transient
    private String byTest;
    
    
    public String getByTest() {
    	return byTest;
    }
    public void setByTest(String byTest) {
    	this.byTest = byTest;
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
