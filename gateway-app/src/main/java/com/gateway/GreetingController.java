package com.gateway;

import java.security.Principal;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.jwtdemo.security.AuthenticatedUser;

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
	
	@GetMapping("/bye")
	@PreAuthorize("hasAuthority('ADMIN')")
	public void admin() {
		
	}
	
	@GetMapping("/resource")
	public void read(@AuthenticationPrincipal AuthenticatedUser user) {
		resourceRestTemplate.exchange(backendUrl, HttpMethod.GET, null, Void.class, Collections.emptyMap());
	}
	@PostMapping("/resource")
	public void write(@AuthenticationPrincipal AuthenticatedUser user) {
		resourceRestTemplate.exchange(backendUrl, HttpMethod.POST, null, Void.class, Collections.emptyMap());
	}
	
	@GetMapping("/backend")
	public void backend() {
		backendRestTemplate.exchange(backendUrl, HttpMethod.GET, null, Void.class, Collections.emptyMap());
	}
	
	
	
}
