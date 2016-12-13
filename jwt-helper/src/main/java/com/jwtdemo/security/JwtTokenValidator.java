package com.jwtdemo.security;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import java.util.Collections;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

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

    private Key key;
    private String requireAudience;
    private String roleClaimName;
        
    public JwtTokenValidator(Key key, String requireAudience, String roleClaimName) {
		super();
		this.key = key;
		
		this.requireAudience = requireAudience;
		this.roleClaimName = roleClaimName;
	}

	public AuthenticatedUser parseToken(String token) { 
    	try {
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
    	JwtParser parser =  Jwts.parser().setSigningKey(key);
    	
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
    
    public static Key buildKey(String key, String algo) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException {
    	if (isPublicKey(key)) { 
    		return fromPEMtoPublicKey(key);
    	}else {
    		return new SecretKeySpec(key.getBytes("UTF-8"), algo == null ? "HS256" : algo);
    	}
    }
    
    private static PublicKey fromPEMtoPublicKey(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] encoded = DatatypeConverter.parseBase64Binary(removeX509Wrapper(pem));

		// PKCS8 decode the encoded RSA private key
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PublicKey pubKey = kf.generatePublic(keySpec);

		return pubKey;
	}
    private static boolean isPublicKey(String cert) {
    	return cert.startsWith("-----BEGIN PUBLIC KEY-----\n");
    }
    private static String removeX509Wrapper(String cert) {
		cert = cert.replace("-----BEGIN PUBLIC KEY-----\n", "");
		return cert.replace("-----END PUBLIC KEY-----", "");
	}
 
}
