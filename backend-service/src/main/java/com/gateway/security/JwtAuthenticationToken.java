package com.gateway.security;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;


/**
 * JWT Token holder required by Spring Security framework (AbstractUserDetailsAuthenticationProvider)
 *
 * @author pascal alma
 */
@SuppressWarnings("serial")
public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private String token;

    public JwtAuthenticationToken(String token) {
        super(null, null);
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }
}