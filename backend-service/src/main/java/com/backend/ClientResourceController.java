package com.backend;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientResourceController {

	@GetMapping
	@PreAuthorize("hasAuthority('backend.read')")
	public void read() {
		
	}
	@RequestMapping(method = { RequestMethod.POST, RequestMethod.PUT })
	@PreAuthorize("hasAuthority('backend.write')")
	public void write() {
		
	}

}
