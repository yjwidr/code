package com.netbrain.autoupdate.apiserver.exception;

public enum ErrorCodes {
    NULL_OBJ(1000,"object is empty"),
    ERROR_ADD_USER(1002,"add user was failed"),
    UNKNOWN_ERROR(1003,"system is busy ,please try later again"),
    NO_PERMISSION(1004,"Permission Denied"),
    ERROR_USERNAME_PASSWORD(1002,"username or password is incorrect"),
    ERROR_USERNAME_NOT_EXISTS(10001,"username not exists"),
    ERROR_USERID_NOT_EXISTS(10002,"userid not exists"),
    ERROR_ROLEID_NOT_EXISTS(10003,"roleId not exists"),
    ERROR_PERMISSIONID_NOT_EXISTS(10004,"permissionIds not exists"),
    ERROR_USERNAME_CANNOT_EMPTY(10005,"username can't empty"),
	ERROR_CONTENTVERSION_ERROR(10006,"content version formate error"),
    ERROR_WHITE_LIST_ID_NOT_EXISTS(10007,"white list id not exists"),
	ERROR_FILE_IS_EMPTY(10008,"file is empty(not exists)");

    private int code;
    private String message;

    private ErrorCodes(int code, String message) {
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