package com.xxx.autoupdate.apiserver.model.parameter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;

public class LoginUser { 
    @NotBlank(message ="userName cannot be empty")
    @Length(min=5, max=128, message="userName length must be between 5-128")
    @Pattern(regexp = "^[A-Za-z]+[\\s\\S]*+$", message = "the userName must begin with an alphabetic or underscore")
    private String userName;
    @Length(max=128)
    @NotBlank(message ="password cannot be empty")
    private String password;

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
