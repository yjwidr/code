package com.netbrain.kc.framework.util;

import com.netbrain.kc.framework.exception.FrameworkErrorCodes;

public class ResponseEntity {

    private final String errorMsg;
    private final int resultCode;
    private Object data;


    public String getErrorMsg() {
        return errorMsg;
    }

    public int getResultCode() {
        return resultCode;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public ResponseEntity(int resultCode, String errorMsg) {
        this.resultCode = resultCode;
        this.errorMsg = errorMsg;
        this.data="";
    }

    public static ResponseEntity ok() {
        return new ResponseEntity(0, "OK");
    }

    public static ResponseEntity ok(Object data) {
        ResponseEntity responseEntity = new ResponseEntity(0, "OK");
        responseEntity.setData(data);
        return responseEntity;
    }

    public static ResponseEntity notFound() {
        return new ResponseEntity(404, "Not Found");
    }

    public static ResponseEntity badRequest() {
        return new ResponseEntity(400, "Bad Request");
    }

    public static ResponseEntity forbidden() {
        return new ResponseEntity(403, "Forbidden");
    }

    public static ResponseEntity unauthorized() {
        return new ResponseEntity(401, "unauthorized");
    }
    
    public static ResponseEntity usernameOrPasswordIncorrect() {
        return new ResponseEntity(FrameworkErrorCodes.ERROR_USERNAME_PASSWORD.getCode(), FrameworkErrorCodes.ERROR_USERNAME_PASSWORD.getMessage());
    }

    public static ResponseEntity serverInternalError() {
        return new ResponseEntity(500, "Server Internal Error");
    }

    public static ResponseEntity customerError() {
        return new ResponseEntity(1001, "Customer Error");
    }
}
