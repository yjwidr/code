package com.xxx.autoupdate.apiserver.exception;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.xxx.autoupdate.apiserver.util.ResponseEntity;


@ControllerAdvice
@ResponseBody
public class ExceptionAdvice {
    private static Logger logger = LogManager.getLogger(ExceptionAdvice.class.getName());

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        logger.error(e.getMessage(),e);
        return new ResponseEntity(HttpStatus.NOT_FOUND.value(),getInitMsg(e));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logger.error(e.getMessage(),e);
        return new ResponseEntity(HttpStatus.NOT_FOUND.value(),getInitMsg(e));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
        String field = error.getField();
        String errorMsg = error.getDefaultMessage();
        String message = String.format("%s:%s", field, errorMsg);
        logger.error(message,e);
        return new ResponseEntity(HttpStatus.BAD_REQUEST.value(),errorMsg);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ResponseEntity handleBindException(BindException e) {
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
        String field = error.getField();
        String errorMsg = error.getDefaultMessage();
        String message = String.format("%s:%s", field, errorMsg);
        logger.error(message,e);
        return new ResponseEntity(HttpStatus.BAD_REQUEST.value(),errorMsg);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity handleServiceException(ConstraintViolationException e) {
        logger.error(e.getMessage(),e);
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        ConstraintViolation<?> violation = violations.iterator().next();
        String message = violation.getMessage();
        return new ResponseEntity(HttpStatus.BAD_REQUEST.value(),message);
    }

//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    @ExceptionHandler(ValidationException.class)
//    public ResponseEntity handleValidationException(ValidationException e) {
//        logger.error(e.getMessage(),e);
//        return new ResponseEntity(HttpStatus.BAD_REQUEST.value(),getInitMsg(e));
//    }
    
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity noHandlerFoundException(NoHandlerFoundException e) {
        logger.error(e.getMessage(),e);
        return new ResponseEntity(HttpStatus.NOT_FOUND.value(),getInitMsg(e));
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        logger.error(e.getMessage(),e);
        return new ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED.value(),getInitMsg(e));
    }

    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        logger.error(e.getMessage(),e);
        return new ResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),getInitMsg(e));
    }    
    
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception e) {
        logger.error(e.getMessage(),e);
        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(),getInitMsg(e));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity handleBusinessException(BusinessException e) {
        logger.error(e.getMessage(),e);
        return new ResponseEntity(e.getCode(),getInitMsg(e));
    } 
    
    private String getInitMsg(Exception e) {
    	if(e instanceof BusinessException) {
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

