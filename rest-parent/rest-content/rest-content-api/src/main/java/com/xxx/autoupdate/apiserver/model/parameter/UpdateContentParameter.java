package com.xxx.autoupdate.apiserver.model.parameter;

import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

public class UpdateContentParameter {
    @NotBlank(message ="id cannot be empty")
    @Length(min=1, max=128, message="id length must be between 1-128")
    private String id;
    @NotBlank(message ="name cannot be empty")
    private String name;
    @NotBlank(message ="description cannot be empty")
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
