package com.xxx.autoupdate.apiserver.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;

import com.xxx.autoupdate.apiserver.progress.CustomMultipartResolver;

@Configuration
public class UploadConfig {
    @Bean(name = "multipartResolver")
    public MultipartResolver multipartResolver() {
        CustomMultipartResolver customMultipartResolver = new CustomMultipartResolver();
        return customMultipartResolver;
    }
}
