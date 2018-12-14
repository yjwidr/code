package com.netbrain.kc.security.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.netbrain.kc.framework.exception.FrameworkErrorCodes;
import com.netbrain.kc.framework.token.JwtToken;
import com.netbrain.kc.security.exception.CustomerAuthenticationException;
import com.netbrain.kc.security.exception.TokenExpiredAuthenticationException;
import com.netbrain.kc.security.model.UserPrincipal;
 

public class JWTAuthenticationFilter extends BasicAuthenticationFilter {
	private static final Logger logger = LogManager.getLogger(JWTAuthenticationFilter.class.getName());
    private UserDetailsService userDetailsService;
    private AuthenticationEntryPoint authenticationEntryPoint;
    public JWTAuthenticationFilter(AuthenticationManager authenticationManager,UserDetailsService userDetailsService,AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager);
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.userDetailsService = userDetailsService;
    }
 
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
    	CustomHttpRequestWrapper nrequest =new CustomHttpRequestWrapper(request);
        String token = nrequest.getHeader("Authorization");
        if (token == null || !token.startsWith("token ")) {
            chain.doFilter(nrequest, response);
            return;
        }
        AbstractAuthenticationToken authentication=null;
        AuthenticationException authenticationException =null;
        try {
        	authentication = getAuthentication(nrequest);
        }catch (Exception failed) {
			SecurityContextHolder.clearContext();
			if(failed instanceof  CustomerAuthenticationException) {
				authenticationException=(CustomerAuthenticationException)failed;
			}else if (failed instanceof  TokenExpiredException) {
				authenticationException=new TokenExpiredAuthenticationException(FrameworkErrorCodes.ERROR_TOKEN_EXPRIED.getCode(),failed.getMessage());
			}else {
				authenticationException=new CustomerAuthenticationException(FrameworkErrorCodes.ERROR_AUTH_OTHER.getCode(),failed.getMessage());
			}
			logger.debug("Authentication request for failed: " + failed);
			authenticationEntryPoint.commence(nrequest, response, authenticationException);
			return;
		}
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(nrequest, response);
    }
 
    private AbstractAuthenticationToken getAuthentication(HttpServletRequest request) throws Exception {
    	
    	AbstractAuthenticationToken authentication =null;
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.isEmpty(authorization)) {
          String[] auth = authorization.split(" ");
          if (auth.length == 2) {
              String authType=auth[0].trim(); 
              String tokenOrLicense=auth[1].trim();
              if (!StringUtils.isEmpty(authType) && authType.equals("token") && !StringUtils.isEmpty(tokenOrLicense)) {
	              String username = JwtToken.unsign(tokenOrLicense, String.class);
	              if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
	                  UserPrincipal principal =(UserPrincipal)userDetailsService.loadUserByUsername(username);
	                  authentication = new UsernamePasswordAuthenticationToken(principal.getUser(), null, principal.getAuthorities());
	                  authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
	                  logger.info("authenticated user " + username + ", setting security context");
	                  SecurityContextHolder.getContext().setAuthentication(authentication);
	              }else if(SecurityContextHolder.getContext().getAuthentication() != null){
	            	  authentication= (AbstractAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
	              }
              }
          }
        }
        return authentication;
    }
}
