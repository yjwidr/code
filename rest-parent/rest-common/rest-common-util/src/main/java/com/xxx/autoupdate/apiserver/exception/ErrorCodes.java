package com.xxx.autoupdate.apiserver.exception;

public enum ErrorCodes {
    NULL_OBJ(10001,"object is empty"),
    UNKNOWN_ERROR(10002,"system is busy ,please try later again"),
    NO_PERMISSION(10003,"Permission Denied"),
    //ERROR_ADD_USER(10004,"add user was failed"),
    ERROR_USERNAME_PASSWORD(10005,"username or password is incorrect"),
    ERROR_USERNAME_NOT_EXISTS(10006,"username not exists"),
    ERROR_USERID_NOT_EXISTS(10007,"userid not exists"),
    ERROR_ROLEID_NOT_EXISTS(10008,"roleId not exists"),
    ERROR_PERMISSIONID_NOT_EXISTS(10009,"permissionIds not exists"),
    ERROR_USERNAME_CANNOT_EMPTY(10010,"username can't empty"),
    //ERROR_CONTENTVERSION_ERROR(10011,"content version formate error"),
    ERROR_WHITE_LIST_ID_NOT_EXISTS(10012,"white list id not exists"),
    ERROR_FILE_IS_EMPTY(10013,"file is empty(or not exists)"),
    ERROR_PUBLISH(10014,"publish failed"),
    ERROR_DISABLE(10015,"disable failed"),
    ERROR_CONTENTNAME_EMPTY(10016,"contentName can't empty"),
    ERROR_SUPPORT_SOFTWARE_VERSION_EMPTY(10017,"supportSoftwareVersion can't empty"),
    ERROR_CONTENTVERSION_EMPTY(10018,"contentVersion can't empty"),
    //ERROR_INFO(10019,"package can't empty"),
    //ERROR_FILE(10020,"package can't empty"),
    ERROR_CONTENT_INVENTORY(10021,"get content inventory error"),
    ERROR_CONTENTVERSION_UPLOAD_ERROR(10022,"upload content version error"),
    ERROR_CONTENT_PACKAGETYPE(10023,"contentPackageType is error, just 1 or 2"),
    ERROR_CONTENT_VERSION_MINOR_INVALID(10024,"the whole content version(%d.%d@%s) minor must greater then %d, but given is %d."),
    ERROR_REVISION_LESSTHAN(10025,"the content version(%d.%d@%s) revision must greater then %d, but given is %d."),
    ERROR_CANT_FIND_WHOLE_CONTENTVERSION(10026,"can't find content version(whole):%d.%d.0@%s"),
    ERROR_CONTENT_VERSION_INVALID(10027,"content version not conform to the regulation."),
    ERROR_WHOLE_PACKAGE_REVISION_MUSTBE_0(10028, "whole package revision must be 0."),
    ERROR_RANGE_OF_CONTENT_VERSION_INVALID(10029,"range of content version invalid."),
	ERROR_TOKEN_EXPRIED(10030,"Token expired."),
	ERROR_AUTH_OTHER(20000,"%s");

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