package com.example.xxx.security;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private static final RequestMatcher PUBLIC_URLS = new OrRequestMatcher(new AntPathRequestMatcher("/user/login"));
    private static final RequestMatcher PROTECTED_URLS = new NegatedRequestMatcher(PUBLIC_URLS);
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser("test").password("$2a$10$07wuX3GHQBADt3wDFBgUceDGJuerb/0sraPAsPFDb1rSmG79pwZWa").authorities("user1");
    }
    @Override
    public void configure(final WebSecurity web) {
      web.ignoring().requestMatchers(PUBLIC_URLS);
    }    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
   	 		http
//   	  			.requestMatchers()
//   	  				.antMatchers("/user/**", "/aaa")
//   	  				.and()
   	  			.authorizeRequests()
   	  				.antMatchers("/user/*", "/aaa").permitAll();
//   	  				.and()
//   	  			.httpBasic();
//        http
//        .sessionManagement()
//        .sessionCreationPolicy(IF_REQUIRED)
//        .and()
//        .csrf().disable()
//            .formLogin()
//            .failureUrl("/api/login?error")
//            .failureForwardUrl("/api/login?error")
////            .loginPage("/login")
////            .loginProcessingUrl("/login")
//            .and()
//            .rememberMe()
////            .usernameParameter("username")
////            .passwordParameter("password")
////            .loginProcessingUrl("/user/login")
////            .and()
////            .requestMatchers().antMatchers("/user/login")
//             .and()
//             .authorizeRequests()
//             .requestMatchers(PROTECTED_URLS).authenticated();
    }
}
