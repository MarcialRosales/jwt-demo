package com.jwtdemo.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class AuthorizationHeaderInjector implements ClientHttpRequestInterceptor {

	private String authorizationHeader;
	
    public AuthorizationHeaderInjector(String token) {
		super();
		this.authorizationHeader = "Bearer " + token;
	}

	@Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        HttpHeaders headers = request.getHeaders();
        headers.add("Authorization", authorizationHeader);
        return execution.execute(request, body);
    }
}