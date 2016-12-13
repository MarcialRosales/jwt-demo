package com.gateway;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.jwtdemo.security.AuthorizationHeaderInjector;
import com.jwtdemo.security.AuthorizationHeaderPropagator;

@SpringBootApplication
public class GatewayAppApplication {

	@Bean(name = "resource")
	public RestTemplate resourceTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new CustomResponseErrorHandler());
		restTemplate.setInterceptors(Collections.singletonList(new AuthorizationHeaderPropagator()));
		return restTemplate;
	}
	
	@Bean(name = "backend")
	public RestTemplate backendTemplate(@Value("${backend.token}") String token) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new CustomResponseErrorHandler());
		restTemplate.setInterceptors(Collections.singletonList(new AuthorizationHeaderInjector(token)));
		return restTemplate;
	}
	
	public static void main(String[] args) {
		SpringApplication.run(GatewayAppApplication.class, args);
	}
}
class CustomResponseErrorHandler implements ResponseErrorHandler {

    private ResponseErrorHandler myErrorHandler = new DefaultResponseErrorHandler();

    public boolean hasError(ClientHttpResponse response) throws IOException {
        return myErrorHandler.hasError(response);
    }

    public void handleError(ClientHttpResponse response) throws IOException {
        switch(response.getRawStatusCode()) {
        case 403:
        case 401:
        	throw new org.springframework.security.access.AccessDeniedException(response.getStatusText());
        }
        myErrorHandler.handleError(response);
    }

}
