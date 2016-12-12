package com.tokenservice;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.KeyGenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.impl.Base64UrlCodec;

@SpringBootApplication
public class JwtTokenServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtTokenServiceApplication.class, args);
	}
}

@RestController
class TokenServiceController {

	@PostMapping("/symmetricalToken")
	public String token(@RequestParam(defaultValue = "HS256") String algo, @RequestParam String symmetricalKey,
			@RequestBody Map<String, Object> claimMap) throws UnsupportedEncodingException {
		Claims claims = Jwts.claims();
		claims.putAll(claimMap);

		SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;
		try {
			algorithm = SignatureAlgorithm.valueOf(algo);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unsupported signature algorithm");
		}

		return Jwts.builder().setClaims(claims).signWith(algorithm, symmetricalKey.getBytes("UTF-8")).compact();
	}

	@GetMapping("/key")
	public String key(@RequestParam(defaultValue = "HmacSHA256") String algo) throws NoSuchAlgorithmException {
		return Base64UrlCodec.BASE64URL.encode(KeyGenerator.getInstance(algo).generateKey().getEncoded());
	}


	@GetMapping("/decode")
	public Map<String, Object> verify(@RequestParam String token, @RequestParam String key )
			throws ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SignatureException,
			IllegalArgumentException, UnsupportedEncodingException {
		Jws<Claims> jwt = parser(key).parseClaimsJws(token);
		Claims body = jwt.getBody();

		return body;
	}

	private JwtParser parser(String key) throws UnsupportedEncodingException {
		JwtParser parser = Jwts.parser().setSigningKey(key.getBytes("UTF-8"));

		return parser;
	}
}