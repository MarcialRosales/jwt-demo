package com.gateway;

import java.security.Principal;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class GreetingController {

	@Autowired
	@Qualifier("resource")
	RestTemplate resourceRestTemplate;
	private @Value("${resource.url:http://localhost:8082/}") String resourceUrl;
	
	@Autowired
	@Qualifier("backend")
	RestTemplate backendRestTemplate;
	private @Value("${backend.url:http://localhost:8082/}") String backendUrl;
	
	@GetMapping("/")
	public String welcome(Principal principal) {
		return "hello " + principal.getName();
	}
	
	@GetMapping("/admin")
	@PreAuthorize("hasAuthority('ADMIN')")
	public String admin(Principal principal) {
		return "executed admin task for " + principal.getName();
	}
	
	@GetMapping("/resource")
	public String read() {
		 ResponseEntity<String> response = resourceRestTemplate.exchange(resourceUrl, HttpMethod.GET, null, String.class, Collections.emptyMap());
		 return response.getBody();
	}
	@PostMapping("/resource")
	public String write() {
		ResponseEntity<String> response =  resourceRestTemplate.exchange(resourceUrl, HttpMethod.POST, null, String.class, Collections.emptyMap());
		return response.getBody();
	}
	
	@GetMapping("/backend")
	public void backend() {
		backendRestTemplate.exchange(backendUrl, HttpMethod.GET, null, Void.class, Collections.emptyMap());
	}
	
	
	
}
