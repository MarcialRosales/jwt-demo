package com.gateway.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

/**
 * Token based authentication filter. It expects the token in the <b>Authorization> header and with the value
 * <b>Bearer TOKEN</b>.
 *   
 */
public class JwtAuthenticationTokenFilter extends AbstractAuthenticationProcessingFilter {

    private String tokenHeader;

    public JwtAuthenticationTokenFilter(String tokenHeader) {
        super("/**");
        this.tokenHeader = tokenHeader;
    }

    /**
     * Attempt to authenticate request - basically just pass over to another method to authenticate request headers
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        
        return getAuthenticationManager().authenticate(extractToken(request));
    }

  
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);

        // As this authentication is in HTTP header, after success we need to continue the request normally
        // and return the response as if the resource was not secured at all
        chain.doFilter(request, response);
    }
    
    public static final String BEARER = "Bearer ";
    
    private JwtAuthenticationToken extractToken(HttpServletRequest request) {
    	// Assumes it has only one token header instance
    	String header = request.getHeader(this.tokenHeader);
    	
        if (header == null || !header.startsWith(BEARER)) {
            throw new AuthenticationCredentialsNotFoundException("JWT token not found");
        }

        return new JwtAuthenticationToken(header.substring(BEARER.length()));

    }
}