package com.netbrain.autoupdate.apiserver.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;

/*
 * Content Package 包含的Resource，每个Resource对应一条记录。
 */
@Entity
@Table(name = "t_resources", uniqueConstraints = { @UniqueConstraint(columnNames = { "key" }) })
public class ResourceEntity implements Serializable {
	private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator="idGenerator")
    @GenericGenerator(name="idGenerator", strategy="uuid")
    private String id; 
    /*
     * 所属的content id
     */
    @Column(nullable=false)
    private String packageId;
    /*
     * Resource key
     */
    @Column(nullable=false, length=5120)
    private String key;
    /*
     * Resource类型，每种资源类型值由类型扩展开发者确定，每种类型值必须在所有类型中唯一
     */
    @Column(nullable=false, length=32)
    private String type;
    @Column(nullable=false)
    private String name;
    /*
     * 资源导出时所属空间，
     * 0：Global 1：Built In，2：Shared in Company，4：Shared in Tenant，8：My Files
     */
    @Column(nullable=false)
    private short space;
    /*
     * 资源所属位置，比如"/Runbook1/"，前不包含空间，后不包含name
     */
    @Column(length=5120)
    private String location;
    /*
     * Resource数据md5 
     */
    @Column(nullable=false, length=32)
    private String dataMd5;
    /*
     * 是否是主要资源，false代表是连带导出的资源
     */
    @Column(nullable=false)
    private boolean isMain;
    
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPackageId() {
		return packageId;
	}
	public void setPackageId(String packageId) {
		this.packageId = packageId;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public short getSpace() {
		return space;
	}
	public void setSpace(short space) {
		this.space = space;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getDataMd5() {
		return dataMd5;
	}
	public void setDataMd5(String dataMd5) {
		this.dataMd5 = dataMd5;
	}
	public boolean isMain() {
		return isMain;
	}
	public void setMain(boolean isMain) {
		this.isMain = isMain;
	}
}
