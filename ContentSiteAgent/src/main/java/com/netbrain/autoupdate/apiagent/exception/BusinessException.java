package com.netbrain.autoupdate.apiagent.exception;

public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1381325479896057076L;

    private int code;
    private int errorType;
    private int errorSubType;

    private String message;

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(int code, String message, int errorType, int errorSubType) {
        super(message);
        this.code = code;
        this.message = message;
        this.errorType = errorType;
        this.errorSubType = errorSubType;
    }

    public BusinessException(int code, String message, int errorType, int errorSubType, Exception innterExc) {
        super(message, innterExc);
        this.code = code;
        this.message = message;
        this.errorType = errorType;
        this.errorSubType = errorSubType;
    }

    public BusinessException(ErrorCodes errorCodes) {
        super(errorCodes.getMessage());
        this.code = errorCodes.getCode();
        this.message = errorCodes.getMessage();
    }
}
