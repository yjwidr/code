package com.xxx.autoupdate.apiserver.security.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.xxx.autoupdate.apiserver.security.service.CustomUserService;

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

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        daoAuthenticationProvider.setUserDetailsService(customUserService());
        return daoAuthenticationProvider;
    }
    @Bean
    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        ProviderManager authenticationManager = new ProviderManager(Arrays.asList(daoAuthenticationProvider()));
        authenticationManager.setEraseCredentialsAfterAuthentication(true);
        return authenticationManager;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new MD5Password();
    }
    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new CustomPermissionEvaluator();
    }

//    @Bean
//    public JwtAuthenticationTokenFilter authenticationTokenFilterBean() throws Exception {
//        return new JwtAuthenticationTokenFilter();
//    }
//    @Override
//    public void configure(final WebSecurity web) {
//      web.ignoring().requestMatchers(PUBLIC_URLS);
//    }
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.authenticationProvider(daoAuthenticationProvider()).userDetailsService(customUserService()).passwordEncoder(passwordEncoder());
        auth.authenticationProvider(daoAuthenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .and()
                .authorizeRequests()
//                .antMatchers("/authorization/token").permitAll()
                .anyRequest().authenticated()
                .and()
//              .authenticationProvider(daoAuthenticationProvider())
//              .addFilterBefore(authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class)
                .addFilter(new JWTLoginFilter(authenticationManager()))
                .addFilter(new JWTAuthenticationFilter(authenticationManager()));
//                .requestMatchers(PROTECTED_URLS)
//                .authenticated()
//                .and()
//                .formLogin().loginProcessingUrl("/authorization/token").successHandler(successHandler());
//                .formLogin().disable()
//                .httpBasic().disable()
//                .logout().disable()
//                .headers()
//                .cacheControl();       
    }
//    @Bean
//    protected TokenAuthenticationFilter restAuthenticationFilter() throws Exception {
//        final TokenAuthenticationFilter filter = new TokenAuthenticationFilter(PROTECTED_URLS);
//        filter.setAuthenticationManager(authenticationManager());
//        filter.setAuthenticationSuccessHandler(successHandler());
//        return filter;
//    }

//    @Bean
//    public SimpleUrlAuthenticationSuccessHandler successHandler() {
//        final SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
//        successHandler.setRedirectStrategy(new NoRedirectStrategy());
//        return successHandler;
//    }
    
//    public static class RestAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
//        @Override
//        public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response, Authentication authentication)throws ServletException, IOException {
//            clearAuthenticationAttributes(request);
//        }
//        @Override
//        public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
//            super.setRedirectStrategy(new NoRedirectStrategy());
//        }
//    }
    
    public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {
        @Override
        protected MethodSecurityExpressionHandler createExpressionHandler() {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(permissionEvaluator());
            return expressionHandler;
        }
    }
}
