package com.netbrain.autoupdate.apiserver.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Permission {
    String[] authorities() default {"Download","Upload","Publish","Push Upgrade","Edit","Disable","Delete"};
}
