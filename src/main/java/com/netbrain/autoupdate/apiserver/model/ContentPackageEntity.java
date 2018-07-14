package com.netbrain.autoupdate.apiserver.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
/*
 * ContentPackage用于存储包文件
 */
@Entity
@Table(name = "t_content_packages")
public class ContentPackageEntity implements Serializable {
	private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator="idGenerator")
    @GenericGenerator(name="idGenerator", strategy="uuid")
    private String id;
    /*
     * Package文件数据
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Type(type="org.hibernate.type.BinaryType")
    @Column(nullable=false)
    private byte[] data;
    /*
     * 所有Resource md5的md5
     */
    @Column(nullable=false, length=32)
    private String dataMd5;
    
    @Transient
    private String version;
    
    @CreatedDate
    @Column(columnDefinition = "timestamp with time zone not null DEFAULT (now())",nullable=false)
    private Date createTime;
    @Column(nullable=false)
    private String createUserId;
    
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public String getDataMd5() {
		return dataMd5;
	}
	public void setDataMd5(String dataMd5) {
		this.dataMd5 = dataMd5;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public String getCreateUserId() {
		return createUserId;
	}
	public void setCreateUserId(String createUserId) {
		this.createUserId = createUserId;
	}
}
