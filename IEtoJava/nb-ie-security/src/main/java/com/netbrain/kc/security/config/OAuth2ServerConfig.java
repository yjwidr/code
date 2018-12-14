package com.netbrain.kc.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.security.web.AuthenticationEntryPoint;

@Configuration
public class OAuth2ServerConfig {

    private static final String DEMO_RESOURCE_ID = "role";

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter { 
    	@Value("${jwt.token.expired.time}")
    	private  Long jwtTokenExpirtedTime;
    	@Autowired
    	AuthenticationManager authenticationManager;
    	
		@Autowired
		@Qualifier("customUserService")
		private UserDetailsService userDetailsService;
		
		@Autowired
		@Qualifier("restAuthenticationEntryPoint")
		private AuthenticationEntryPoint restAuthenticationEntryPoint;
		
        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId(DEMO_RESOURCE_ID).stateless(true);
            resources.authenticationEntryPoint(restAuthenticationEntryPoint);
        }

        @Override
        // @formatter:off
        public void configure(HttpSecurity http) throws Exception {
            http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(restAuthenticationEntryPoint)
            .and()
            .userDetailsService(userDetailsService)
            .authorizeRequests()
            .antMatchers("/oauth/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilter(new JWTLoginFilter(authenticationManager,jwtTokenExpirtedTime))
            .addFilter(new JWTAuthenticationFilter(authenticationManager,userDetailsService,restAuthenticationEntryPoint));
        }
        // @formatter:on
    }

    @Configuration
    @EnableAuthorizationServer
    protected static class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
    	@Value("${oauth2.token.expired.time}")
    	private  int oauth2TokenExpirtedTime;
    	
		@Autowired
		AuthenticationManager authenticationManager;
		
		@Autowired
		RedisConnectionFactory redisConnectionFactory;
		
		@Autowired
		@Qualifier("customUserService")
		private UserDetailsService userDetailsService;
		
		@Autowired
		@Qualifier("restAuthenticationEntryPoint")
		private AuthenticationEntryPoint restAuthenticationEntryPoint;
		
	    @Bean
	    public AuthorizationServerTokenServices tokenServices() {
	        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
	        defaultTokenServices.setAccessTokenValiditySeconds(oauth2TokenExpirtedTime);
	        defaultTokenServices.setRefreshTokenValiditySeconds(oauth2TokenExpirtedTime);
	        defaultTokenServices.setSupportRefreshToken(true);
	        defaultTokenServices.setReuseRefreshToken(true);
	        defaultTokenServices.setTokenStore(new RedisTokenStore(redisConnectionFactory));
	        return defaultTokenServices;
	    }
	    
        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        	 String finalSecret = new MD5Password().encode("123456");
            clients.inMemory().withClient("client_1")
                    .resourceIds(DEMO_RESOURCE_ID)
                    .authorizedGrantTypes("client_credentials", "refresh_token")
                    .scopes("select")
                    .authorities("read","write")
                    .secret(finalSecret)
                    .and().withClient("client_2")
                    .resourceIds(DEMO_RESOURCE_ID)
                    .authorizedGrantTypes("password", "refresh_token")
                    .scopes("select")
                    .authorities("read","write")
                    .secret(finalSecret);
        }

        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
            endpoints
                    .tokenServices(tokenServices())
                    .authenticationManager(authenticationManager)
                    .userDetailsService(userDetailsService)
                    .allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST);
        }

        @Override
        public void configure(AuthorizationServerSecurityConfigurer oauthServer) {
            oauthServer.allowFormAuthenticationForClients();
        }

    }

}
