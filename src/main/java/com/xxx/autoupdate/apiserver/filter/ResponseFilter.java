package com.xxx.autoupdate.apiserver.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//don't use this filter,some problem when contentController download the zip file
//@WebFilter(filterName = "responseFilter", urlPatterns = "/*")
//@Order(1)
public class ResponseFilter implements Filter {
    private static Logger logger = LogManager.getLogger(ResponseFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
//        ResponseEntity responseEntity = ResponseEntity.ok();
        ResponseWrapper responseWrapper = new ResponseWrapper((HttpServletResponse) servletResponse);
        filterChain.doFilter(servletRequest, responseWrapper);
        String responseContent = new String(responseWrapper.getContent());
//        if(!responseContent.contains("code")) {
//            ObjectMapper mapper = new ObjectMapper();
//            servletResponse.setContentType("application/json; charset=utf-8");
//            responseEntity.setData(mapper.readValue(responseContent, String.class));
//            String json = mapper.writeValueAsString(responseEntity);
//            PrintWriter out = servletResponse.getWriter();
//            out.print(json);
//            out.flush();
//            out.close();
//        }else {
            ServletOutputStream out = servletResponse.getOutputStream();
            out.write(responseContent.getBytes());
            out.flush();
            out.close();
//        }
        logger.info("filter==========================");
    }

    @Override
    public void destroy() {

    }
}