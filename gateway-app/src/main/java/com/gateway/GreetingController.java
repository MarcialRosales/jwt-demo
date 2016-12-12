package com.gateway;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

	@GetMapping("/")
	public String welcome(Principal principal) {
		return "hello " + principal.getName();
	}
}
