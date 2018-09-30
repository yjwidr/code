package com.xxx.autoupdate.apiserver.model.parameter;

import java.io.Serializable;
import java.util.List;

public class ResourceItem implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String key;
	private String type;
	private String name;
	private short space;
	/*
	 * 在资源包中相对类型ResourceType文件夹的路径及名称，如"./qapp1.json"
	 */
	private String filePath;
	/*
	 * 资源数据所占大小，不包括依赖的所有资源，单位Byte
	 */
	public long dataSize;
	/*
	 * 直接依赖的ResourceKey数组
	 */
	private List<String> dependencies;
	/*
	 * 资源所属位置，比如"/Runbook1/"，前不包含空间，后不包含name，如需要完整Path，需组合space、name生成
	 */
	private String location;
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
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public long getDataSize() {
		return dataSize;
	}
	public void setDataSize(long dataSize) {
		this.dataSize = dataSize;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public List<String> getDependencies() {
		return dependencies;
	}
	public void setDependencies(List<String> dependencies) {
		this.dependencies = dependencies;
	}
}
