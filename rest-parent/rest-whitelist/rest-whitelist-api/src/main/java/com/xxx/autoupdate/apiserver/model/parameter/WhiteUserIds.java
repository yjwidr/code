package com.xxx.autoupdate.apiserver.model.parameter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class WhiteUserIds {
    @Size(min= 1,message ="whiteUserIds cannot be empty")
    @NotEmpty(message ="whiteUserIds cannot be empty")
    private String[] whiteUserIds;

    public String[] getWhiteUserIds() {
        return whiteUserIds;
    }

    public void setWhiteUserIds(String[] whiteUserIds) {
        this.whiteUserIds = whiteUserIds;
    }


}
