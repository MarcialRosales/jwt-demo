package com.tokenservice;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@SpringBootApplication
public class JwtTokenServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtTokenServiceApplication.class, args);
	}
}

@RestController
class TokenServiceController {
	
	@PostMapping("/symmetricalToken")
	public String token(@RequestParam(defaultValue = "HS256") String algo, 
			@RequestParam String symmetricalKey, @RequestBody Map<String, Object> claimMap) throws UnsupportedEncodingException {
		 Claims claims = Jwts.claims();
		 claims.putAll(claimMap);
	        
		 SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;
		 try { 	
			 algorithm = SignatureAlgorithm.valueOf(algo);
		 }catch(Exception e) {
			 throw new IllegalArgumentException("Unsupported signature algorithm"); 
		 }
		 
	     return Jwts.builder()
	                .setClaims(claims)
	                .signWith(algorithm, symmetricalKey.getBytes("UTF-8"))
	                .compact();
	}
}