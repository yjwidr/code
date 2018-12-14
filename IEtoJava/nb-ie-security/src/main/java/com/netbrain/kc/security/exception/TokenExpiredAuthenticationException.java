package com.netbrain.kc.security.exception;

import org.springframework.security.core.AuthenticationException;

import com.netbrain.kc.framework.exception.FrameworkErrorCodes;


public class TokenExpiredAuthenticationException extends AuthenticationException {

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
  
    public TokenExpiredAuthenticationException(int code,String message) {
        super(message);  
        this.code = code;  
        this.message = message;  
    } 
    
    public TokenExpiredAuthenticationException(FrameworkErrorCodes errorCodes) {
        super(errorCodes.getMessage());  
        this.code = errorCodes.getCode();  
        this.message = errorCodes.getMessage();  
    }
    public TokenExpiredAuthenticationException(FrameworkErrorCodes errorCodes, Object... args) {
        super(String.format(errorCodes.getMessage(),args));  
        this.code = errorCodes.getCode();  
        this.message = super.getMessage();  
    }
}  
