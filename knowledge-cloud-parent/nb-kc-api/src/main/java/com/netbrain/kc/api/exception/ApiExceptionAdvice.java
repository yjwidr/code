package com.netbrain.kc.api.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.netbrain.kc.framework.util.ResponseEntity;


@ControllerAdvice
@ResponseBody
public class ApiExceptionAdvice {
    private static Logger logger = LogManager.getLogger(ApiExceptionAdvice.class.getName());
 
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity handleApiException(ApiException e) {
    	logger.error(e.getMessage(),e);
    	return new ResponseEntity(e.getCode(),getInitMsg(e));
    } 
    
    private String getInitMsg(Exception e) {
    	if(e instanceof ApiException) {
    		return e.getMessage();
    	}else {
    		Throwable ex = e;
    		while(ex.getCause()!=null) {
    			ex = ex.getCause();
    		}
    		return ex.getMessage();
    	}
    }
    
}

