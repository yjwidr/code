package com.netbrain.autoupdate.apiagent.entity;

public class ErrorReport {
    private int errorType;
    private int errorSubType;
    private String errorMsg;
    private String errorDetails;
    public int getErrorType() {
        return errorType;
    }
    public void setErrorType(int errorType) {
        this.errorType = errorType;
    }
    public int getErrorSubType() {
        return errorSubType;
    }
    public void setErrorSubType(int errorSubType) {
        this.errorSubType = errorSubType;
    }
    public String getErrorMsg() {
        return errorMsg;
    }
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    public String getErrorDetails() {
        return errorDetails;
    }
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
