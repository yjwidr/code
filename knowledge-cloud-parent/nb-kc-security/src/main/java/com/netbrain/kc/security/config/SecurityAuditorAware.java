package com.netbrain.kc.security.config;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.netbrain.kc.api.model.datamodel.UserEntity;
@Component
public class SecurityAuditorAware implements AuditorAware<String> {
	public Optional<String> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		String id=((UserEntity)authentication.getPrincipal()).getId();
		Optional<String> opt = Optional.of(id);
		return opt;
	}
}
