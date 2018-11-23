package com.netbrain.kc.framework.exception;

public enum FrameworkErrorCodes {
    ERROR_USERNAME_PASSWORD(10005,"username or password is incorrect"),
    ERROR_TOKEN_EXPRIED(10030,"Token expired."),
    ERROR_USERNAME_NOT_EXISTS(10006,"username not exists"),
	ERROR_AUTH_OTHER(20000,"%s");
    private int code;
    private String message;

    private FrameworkErrorCodes(int code, String message) {
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