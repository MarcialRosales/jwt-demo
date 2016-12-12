package com.resource;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientResourceController {

	@GetMapping
	@PreAuthorize("hasAuthority('resource.read')")
	public String read(Principal principal) {
		return String.format("read %s's resource", principal.getName()); 
	}
	@RequestMapping(method = { RequestMethod.POST, RequestMethod.PUT })
	@PreAuthorize("hasAuthority('resource.write')")
	public String write(Principal principal) {
		return String.format("wrote %s's resource", principal.getName());
	}

}
