package com.gateway.security;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;


/**
 * Uses the token to authenticate the user and provide its user details contained within the token
 *
 */
public class JwtAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

	private JwtTokenValidator jwtTokenValidator;

    public JwtAuthenticationProvider(JwtTokenValidator jwtTokenValidator) {
		super();
		this.jwtTokenValidator = jwtTokenValidator;
	}
    
    @Override
    public boolean supports(Class<?> authentication) {
        return (JwtAuthenticationToken.class.isAssignableFrom(authentication));
    }


    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;
        
        return jwtTokenValidator.parseToken(jwtAuthenticationToken.getToken());

    }

    @Override
	protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
    }

}
