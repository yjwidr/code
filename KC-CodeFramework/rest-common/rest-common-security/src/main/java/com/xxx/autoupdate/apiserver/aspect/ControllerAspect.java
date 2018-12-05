package com.xxx.autoupdate.apiserver.aspect;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSON;

@Aspect
@Component
public class ControllerAspect {

    private static Logger logger = LogManager.getLogger(ControllerAspect.class.getName());

    @Pointcut("execution(public * com.xxx.autoupdate.apiserver.controller.*.*(..))")
    public void privilege() {
    }

    @Before("privilege()")
    public void doBefore(JoinPoint joinPoint){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request =  attributes.getRequest();
        logger.info("url ={}",request.getRequestURI());
        logger.info("method={}",request.getMethod());
        logger.info("ip={}",request.getRemoteAddr());
        logger.info("ip={}",request.getHeader("X-Real-IP"));
        logger.info("class_method={}",joinPoint.getSignature().getDeclaringTypeName()+'.'+ joinPoint.getSignature().getName());//获取类名及类方法
        logger.info("args={}",joinPoint.getArgs().toString());
    }    
    
    /**
     * @param joinPoint
     * @throws Throwable
     */
    @ResponseBody
    @Around("privilege()")
    public Object isAccessMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    	return joinPoint.proceed();
    }
    @AfterReturning(returning = "obj",pointcut = "privilege()")
    public void doAfterReturning(Object obj){
        logger.info("response={}",JSON.toJSONString(obj));
    }
}