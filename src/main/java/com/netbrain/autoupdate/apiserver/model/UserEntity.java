package com.netbrain.autoupdate.apiserver.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonFilter;

@Entity
@Table(name = "t_users", uniqueConstraints = { @UniqueConstraint(columnNames = { "loginName" }) })
@JsonFilter("com.netbrain.autoupdate.apiserver.model.UserEntity")
public class UserEntity implements Serializable{
    private static final long serialVersionUID = 1L; 
    @Id
    @GeneratedValue(generator = "idGenerator")
    @GenericGenerator(name = "idGenerator", strategy = "uuid")
    private String id;
    @NotBlank(message ="userName cannot be empty")
    @Length(min=5, max=128, message="userName length must be between 5-128")
    @Pattern(regexp = "^[A-Za-z]+[\\s\\S]*+$", message = "the userName must begin with an alphabetic or underscore")
    @Column(nullable=false,length=128)
    private String userName;
    @NotBlank(message ="loginName cannot be empty")
    @Length(min=5, max=128, message="loginName length must be between 5-128")
    @Pattern(regexp = "^[A-Za-z]+[\\s\\S]*+$", message = "the loginName must begin with an alphabetic or underscore")
    @Column(nullable=false,length=128)
    private String loginName;
    @Length(max=48)
    @NotBlank(message ="password cannot be empty")
    @Column(nullable=false,length=128)
    private String password;
    @Column(length=128)
    private String email;
    @NotBlank(message ="licenceId cannot be empty")
    @Column(length=128)
    private String licenceId;
    @NotBlank(message ="company cannot be empty")
    @Column(length=1024)
    private String company;
    @Column(length=5120)
    private String description;
    @Column(nullable=false) 
    private String roleId;
    @Transient
    private List<String> authorities;
    
    @CreatedDate
    @Column(columnDefinition = "timestamp with time zone not null DEFAULT (now())",nullable=false)
    private Date createTime;
    @LastModifiedDate
    @Column(columnDefinition = "timestamp with time zone not null DEFAULT (now())",nullable=false)
    private Date updateTime;
    @Column(nullable=false)
    private String createUserId;
    @Column(nullable=false)
    private String updateUserId;

    public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getCompany() {
        return company;
    }
    public void setCompany(String company) {
        this.company = company;
    }

    public List<String> getAuthorities() {
        return authorities;
    }
    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getLoginName() {
        return loginName;
    }
    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }
    public String getLicenceId() {
        return licenceId;
    }
    public void setLicenceId(String licenceId) {
        this.licenceId = licenceId;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getRoleId() {
        return roleId;
    }
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public Date getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	public String getCreateUserId() {
		return createUserId;
	}
	public void setCreateUserId(String createUserId) {
		this.createUserId = createUserId;
	}
	public String getUpdateUserId() {
		return updateUserId;
	}
	public void setUpdateUserId(String updateUserId) {
		this.updateUserId = updateUserId;
	}
    
}
