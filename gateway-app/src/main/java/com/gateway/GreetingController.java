package com.gateway;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

	@GetMapping("/")
	public String welcome(Principal principal) {
		return "hello " + principal.getName();
	}
	
	@GetMapping("/bye")
	@PreAuthorize("hasAuthority('ADMIN')")
	public void admin() {
		
	}
}
