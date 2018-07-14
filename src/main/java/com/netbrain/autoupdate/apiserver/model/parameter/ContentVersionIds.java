package com.netbrain.autoupdate.apiserver.model.parameter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class ContentVersionIds {
    @Size(min= 1,message ="contentVersionIds cannot be empty")
    @NotEmpty(message ="contentVersionIds cannot be empty")
    private String[] contentVersionIds;

    public String[] getContentVersionIds() {
        return contentVersionIds;
    }

    public void setContentVersionIds(String[] contentVersionIds) {
        this.contentVersionIds = contentVersionIds;
    }
}
