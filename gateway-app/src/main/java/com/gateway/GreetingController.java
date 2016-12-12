package com.gateway;

import java.security.Principal;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.gateway.security.AuthenticatedUser;

@RestController
public class GreetingController {

	@Autowired
	RestTemplate restTemplate;
	
	@Value("${backendUrl:http://localhost:8082/}")
	String backendUrl;
	
	
	@GetMapping("/")
	public String welcome(Principal principal) {
		return "hello " + principal.getName();
	}
	
	@GetMapping("/bye")
	@PreAuthorize("hasAuthority('ADMIN')")
	public void admin() {
		
	}
	
	@GetMapping("/backend")
	public void read(@AuthenticationPrincipal AuthenticatedUser user) {
		restTemplate.exchange(backendUrl, HttpMethod.GET, null, Void.class, Collections.emptyMap());
	}
	@PostMapping("/backend")
	public void write(@AuthenticationPrincipal AuthenticatedUser user) {
		restTemplate.exchange(backendUrl, HttpMethod.POST, null, Void.class, Collections.emptyMap());
	}
	
	
}
