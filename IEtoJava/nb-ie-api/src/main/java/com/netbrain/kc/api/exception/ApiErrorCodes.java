package com.netbrain.kc.api.exception;

public enum ApiErrorCodes {
    ERROR_USERNAME_PASSWORD(10005,"username or password is incorrect"),
    ERROR_USERNAME_NOT_EXISTS(10006,"username not exists"),
    ERROR_USERID_NOT_EXISTS(10007,"userid not exists"),
    ERROR_ROLEID_NOT_EXISTS(10008,"roleId not exists"),
    ERROR_PERMISSIONID_NOT_EXISTS(10009,"permissionIds not exists"),
    ERROR_USERNAME_CANNOT_EMPTY(10010,"username can't empty");

    private int code;
    private String message;

    private ApiErrorCodes(int code, String message) {
        this.setCode(code);
        this.setMessage(message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "[" + this.code + "]" + this.message;
    }
}