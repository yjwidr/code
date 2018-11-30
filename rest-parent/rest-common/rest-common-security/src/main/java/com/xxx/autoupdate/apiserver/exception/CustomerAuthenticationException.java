package com.xxx.autoupdate.apiserver.exception;

import org.springframework.security.core.AuthenticationException;

public class CustomerAuthenticationException extends AuthenticationException {

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
  
    public CustomerAuthenticationException(int code,String message) {
        super(message);  
        this.code = code;  
        this.message = message;  
    } 
    
    public CustomerAuthenticationException(ErrorCodes errorCodes) {
        super(errorCodes.getMessage());  
        this.code = errorCodes.getCode();  
        this.message = errorCodes.getMessage();  
    }
    public CustomerAuthenticationException(ErrorCodes errorCodes, Object... args) {
        super(String.format(errorCodes.getMessage(),args));  
        this.code = errorCodes.getCode();  
        this.message = super.getMessage();  
    }
}  
