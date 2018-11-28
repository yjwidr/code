package com.netbrain.kc.api.exception;

public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1381325479896057076L;  

    private int code;  
   
    private String message;  
  
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
  
    public ApiException(int code,String message) {
        super(message);  
        this.code = code;  
        this.message = message;  
    } 
    
    public ApiException(ApiErrorCodes errorCodes) {
        super(errorCodes.getMessage());  
        this.code = errorCodes.getCode();  
        this.message = errorCodes.getMessage();  
    } 
}  
