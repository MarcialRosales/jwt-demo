package com.tokenservice;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.xml.bind.DatatypeConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.json.JsonParser;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.Base64UrlCodec;

@SpringBootApplication
public class JwtTokenServiceApplication {

	@Bean
	public JsonParser jsonParser() {
		return new JacksonJsonParser();
	}

	public static void main(String[] args) {
		SpringApplication.run(JwtTokenServiceApplication.class, args);
	}
}

@RestController
class TokenServiceController {

	@Autowired
	org.springframework.boot.json.JsonParser jsonParser;

	@PostMapping("/token")
	public String token(@RequestParam(required = false) String algo, @RequestParam("claims") String claimJson,
			@RequestParam(required = false) String symkey, @RequestParam(required = false) MultipartFile asymkey)
			throws Exception {

		if (symkey == null && asymkey == null) {
			throw new IllegalArgumentException("missing symKey or asymKey");
		}
		if (algo == null) {
			algo = symkey != null ? "HS256" : "RS256";
		}
		SignatureAlgorithm algorithm = getAlgorithm(algo);
		if (algorithm.isHmac()) {
			return sign(build(claimJson), algorithm, symkey).compact();
		} else {
			return sign(build(claimJson), algorithm, fromPEM2Key(new String(asymkey.getBytes()))).compact();
		}
	}

	private SignatureAlgorithm getAlgorithm(String algo) {
		try {
			return SignatureAlgorithm.valueOf(algo);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unsupported signature algorithm");
		}
	}

	private JwtBuilder sign(JwtBuilder jwt, SignatureAlgorithm algo, String key) throws UnsupportedEncodingException {
		return jwt.signWith(algo, key.getBytes("UTF-8"));
	}

	private JwtBuilder sign(JwtBuilder jwt, SignatureAlgorithm algo, Key key) throws UnsupportedEncodingException {
		return jwt.signWith(algo, key);
	}

	private JwtBuilder build(String claimsJson) {
		return Jwts.builder().setClaims(jsonParser.parseMap(claimsJson));
	}

	private String removePKCKS8PEMWrapper(String pem) {
		pem = pem.replace("-----BEGIN PRIVATE KEY-----\n", "");
		return pem.replace("-----END PRIVATE KEY-----", "");
	}
	private String removeX509Wrapper(String cert) {
		cert = cert.replace("-----BEGIN PUBLIC KEY-----\n", "");
		return cert.replace("-----END PUBLIC KEY-----", "");
	}
	private PrivateKey fromPEM2Key(String pem)
			throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
		byte[] encoded = DatatypeConverter.parseBase64Binary(removePKCKS8PEMWrapper(pem));

		// PKCS8 decode the encoded RSA private key
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PrivateKey privKey = kf.generatePrivate(keySpec);

		return privKey;
	}
	private PublicKey fromPEM2PublicKey(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] encoded = DatatypeConverter.parseBase64Binary(removeX509Wrapper(pem));

		// PKCS8 decode the encoded RSA private key
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PublicKey pubKey = kf.generatePublic(keySpec);

		return pubKey;
	}
	@GetMapping("/key")
	public String key(@RequestParam(defaultValue = "HmacSHA256") String algo) throws NoSuchAlgorithmException {
		return Base64UrlCodec.BASE64URL.encode(KeyGenerator.getInstance(algo).generateKey().getEncoded());
	}

	@PostMapping("/verify")
	public Map<String, Object> verify(@RequestParam(required = false) String symkey,
			@RequestParam(required = false) MultipartFile asymkey, @RequestParam String token)
			throws Exception {
		if (symkey == null && asymkey == null) {
			throw new IllegalArgumentException("missing symKey or asymKey");
		}
		
		JwtParser parser = Jwts.parser();
		if (asymkey != null) {
			parser.setSigningKey(fromPEM2PublicKey(new String(asymkey.getBytes())));
		}else {
			parser.setSigningKey(symkey.getBytes("UTF-8"));
		}
		Claims body = parser.parseClaimsJws(token).getBody();

		return body;
	}

	
}