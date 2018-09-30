package com.xxx.autoupdate.apiserver.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.xxx.autoupdate.apiserver.security.service.CustomUserService;
import com.xxx.autoupdate.apiserver.services.UserService;

//https://blog.csdn.net/linzhiqiang0316/article/details/78358907
//http://357029540.iteye.com/blog/2329730
//https://www.baeldung.com/spring-security-create-new-custom-security-expression
//https://blog.csdn.net/greedystar/article/details/81220070
//https://www.imooc.com/article/22886
//https://github.com/jloisel
//https://octoperf.com/blog/2018/03/08/securing-rest-api-spring-security/
//https://blog.csdn.net/greedystar/article/details/81220070
//https://github.com/GreedyStar/SpringBootDemo

//https://blog.csdn.net/sxdtzhaoxinguo/article/details/77965226
//http://godjohnny.iteye.com/blog/2319877
//http://www.cnblogs.com/softidea/p/6229553.html
//https://blog.csdn.net/iverson2010112228/article/details/52837579
//http://projects.spring.io/spring-security-oauth/docs/oauth2.html
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
//    private static final RequestMatcher PUBLIC_URLS = new OrRequestMatcher(new AntPathRequestMatcher("/authorization/token"));
//    private static final RequestMatcher PROTECTED_URLS = new NegatedRequestMatcher(PUBLIC_URLS);


    @Bean
    protected UserDetailsService customUserService() {
        return new CustomUserService();
    }
    
    @Value("${token.expired.time}")
    private Long expirtedTime;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new MD5Password();
    }
    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new CustomPermissionEvaluator();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(new CustomAuthenticationProvider(customUserService(), passwordEncoder()));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(new RestAuthenticationEntryPoint())
            .and()
            .authorizeRequests()
            .anyRequest().authenticated()
            .and()
            .addFilter(new JWTLoginFilter(authenticationManager(),expirtedTime))
            .addFilter(new JWTAuthenticationFilter(authenticationManager(),customUserService()));
    }
  
    public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {
        @Override
        protected MethodSecurityExpressionHandler createExpressionHandler() {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(permissionEvaluator());
            return expressionHandler;
        }
    }
}
