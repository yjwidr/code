package com.netbrain.autoupdate.apiserver.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "t_permissions",uniqueConstraints = { @UniqueConstraint(columnNames = {"name"})})
public class PermissionEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator="idGenerator")
    @GenericGenerator(name="idGenerator", strategy="uuid")
    private String id; 
    @NotBlank(message ="name cannot be empty")
    @Length(max=128)
    @Column(nullable=false,length=128)
    private String name;
	@Column(length=5120)
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
