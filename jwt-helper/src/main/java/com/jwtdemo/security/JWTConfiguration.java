package com.jwtdemo.security;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
class JWTConfiguration {

	@NotNull
	private String key;

	private String keyAlgorithm;
	
	private String requireAudience;

	private String roleClaimName = "roles";

    private String tokenHeader = "Authorization";

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getRequireAudience() {
		return requireAudience;
	}

	public void setRequireAudience(String requireAudience) {
		this.requireAudience = requireAudience;
	}

	public String getRoleClaimName() {
		return roleClaimName;
	}

	public void setRoleClaimName(String roleClaimName) {
		this.roleClaimName = roleClaimName;
	}

	public String getTokenHeader() {
		return tokenHeader;
	}

	public void setTokenHeader(String tokenHeader) {
		this.tokenHeader = tokenHeader;
	}

	public String getKeyAlgorithm() {
		return keyAlgorithm;
	}

	public void setKeyAlgorithm(String keyAlgorithm) {
		this.keyAlgorithm = keyAlgorithm;
	}
}