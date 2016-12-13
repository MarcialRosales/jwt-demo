package com.jwtdemo.security;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;


public class JwtTokenValidator {

    private String secret;
    private String requireAudience;
    private String roleClaimName;
    
    private static Logger log = org.slf4j.LoggerFactory.getLogger(JwtTokenValidator.class);
    
    public JwtTokenValidator(String secret, String requireAudience, String roleClaimName) {
		super();
		this.secret = secret;
		this.requireAudience = requireAudience;
		this.roleClaimName = roleClaimName;
	}

	public AuthenticatedUser parseToken(String token) {
    	try {
        	log.debug("using key: [%s] to validate token \n%s", secret, token);

            Jws<Claims> jwt = parser().parseClaimsJws(token);
            Claims body = jwt.getBody();
            
            // Security measure: Make sure the alg header contains at least one signature algorithm
            if (StringUtils.isEmpty(jwt.getHeader().getAlgorithm()) || 
            		SignatureAlgorithm.NONE.equals(jwt.getHeader().getAlgorithm())) {
            	throw new BadCredentialsException("JWT must be digitally signed");
            }
            
            // Security measure: Make sure the token has a subject
            if (body.getSubject() == null) {
            	throw new BadCredentialsException("Missing subject");
            }
            
            // Security measure: Enforce date validation
            // TODO
            
           return  new AuthenticatedUser(body.getSubject(), token, buildAuthorities(body));
            
        } catch (JwtException | UnsupportedEncodingException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }
    
    // Once it is confirmed that JwtParser is thread-safe we can have a single instance
    // https://github.com/jwtk/jjwt/issues/171
    private JwtParser parser() throws UnsupportedEncodingException {
    	JwtParser parser =  Jwts.parser().setSigningKey(secret.getBytes("UTF-8"));
    	
    	if (requireAudience != null) {
    		parser.requireAudience(requireAudience);
    	}
    	
    	return parser;
    }
    private Collection<? extends GrantedAuthority> buildAuthorities(Claims body) {
    	if (roleClaimName == null || !body.containsKey(roleClaimName)) {
    		return Collections.emptyList();
    	}
    	
    	return AuthorityUtils.commaSeparatedStringToAuthorityList(body.get(roleClaimName, String.class));	
    	
    }
    
 
}
