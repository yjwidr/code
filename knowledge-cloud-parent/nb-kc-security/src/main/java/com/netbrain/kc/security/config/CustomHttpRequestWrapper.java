package com.netbrain.kc.security.config;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class CustomHttpRequestWrapper extends  HttpServletRequestWrapper {

    CustomHttpRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public Locale getLocale() {
        return Locale.ENGLISH;
    }

}