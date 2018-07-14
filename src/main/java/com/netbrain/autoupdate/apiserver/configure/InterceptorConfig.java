package com.netbrain.autoupdate.apiserver.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.netbrain.autoupdate.apiserver.interceptor.InterceptorToken;

@Configuration
public class InterceptorConfig extends WebMvcConfigurationSupport {
    @Bean
    public InterceptorToken interceptorToken() {
        return new InterceptorToken();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptorToken())
                .excludePathPatterns("/static/*")
                .excludePathPatterns("/authorization/token")
                .addPathPatterns("/**");
        super.addInterceptors(registry);
    }
}
