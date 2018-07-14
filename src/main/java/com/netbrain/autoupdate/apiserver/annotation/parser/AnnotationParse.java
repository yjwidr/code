package com.netbrain.autoupdate.apiserver.annotation.parser;

import java.lang.reflect.Method;

import com.netbrain.autoupdate.apiserver.annotation.Permission;

public class AnnotationParse {

    public static String[] privilegeParse(Method method) throws Exception {
        if (method.isAnnotationPresent(Permission.class)) {
            Permission annotation = method.getAnnotation(Permission.class);
            return annotation.authorities();
        }
        return null;
    }
}
