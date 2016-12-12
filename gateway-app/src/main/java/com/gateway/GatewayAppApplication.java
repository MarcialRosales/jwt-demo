package com.gateway;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.jwtdemo.security.AuthenticatedUser;
import com.jwtdemo.security.JwtAuthenticationProvider;
import com.jwtdemo.security.JwtAuthenticationTokenFilter;
import com.jwtdemo.security.JwtTokenValidator;

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
        	throw new org.springframework.security.access.AccessDeniedException(response.getStatusText());
        }
        myErrorHandler.handleError(response);
    }

}
class AuthorizationHeaderPropagator implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        HttpHeaders headers = request.getHeaders();
        AuthenticatedUser user = (AuthenticatedUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        headers.add("Authorization", "Bearer " + user.getToken());
        return execution.execute(request, body);
    }
}
class AuthorizationHeaderInjector implements ClientHttpRequestInterceptor {

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
/**
 * Security configuration: - Use method annotation @PreAuthorize to declare the
 * required role to execute the method
 * 
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true) // only required if we are
													// going to do role-based
													// authentication
@EnableWebSecurity
class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	JWTConfiguration jwtConfiguration;
	
	AuthenticationEntryPoint handleUnauthenticatedUsers() {
		return (HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) -> {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
		};
	}

	AuthenticationSuccessHandler handleAuthenticatedUsers() {
		return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {

		};
	}

	AuthenticationProvider jwtBasedAuthentication() {
		return new JwtAuthenticationProvider(new JwtTokenValidator(jwtConfiguration.getSecret(), jwtConfiguration.getRequireAudience(), 
				jwtConfiguration.getRoleClaimName()));
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManager() throws Exception {
		return new ProviderManager(Arrays.asList(jwtBasedAuthentication()));
	}

	@Bean
	public JwtAuthenticationTokenFilter authenticationTokenFilterBean() throws Exception {
		JwtAuthenticationTokenFilter authenticationTokenFilter = new JwtAuthenticationTokenFilter(jwtConfiguration.getTokenHeader());
		authenticationTokenFilter.setAuthenticationManager(authenticationManager());
		authenticationTokenFilter.setAuthenticationSuccessHandler(handleAuthenticatedUsers());
		return authenticationTokenFilter;
	}

	@Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {
		httpSecurity
				// REST-api doesn't need CSRF
				.csrf().disable()
				.authorizeRequests().anyRequest().authenticated()
				.and().exceptionHandling().authenticationEntryPoint(handleUnauthenticatedUsers())
				.and().httpBasic().disable();
				

		disableHttpSession(httpSecurity);
		enableJWTBasedAuthentication(httpSecurity);
		disablePageCaching(httpSecurity);

	}

 
	private void enableJWTBasedAuthentication(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.addFilterBefore(authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class);
	}

	private void disablePageCaching(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.headers().cacheControl();
	}

	private void disableHttpSession(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
	}
}


@Configuration
@ConfigurationProperties(prefix = "jwt")
class JWTConfiguration {

	@NotNull
	private String secret;

	private String requireAudience;

	private String roleClaimName = "roles";

    private String tokenHeader = "Authorization";

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getRequireAudience() {
		return requireAudience;
	}

	public void setRequireAudience(String requireAudience) {
		this.requireAudience = requireAudience;
	}

	public String getRoleClaimName() {
		return roleClaimName;
	}

	public void setRoleClaimName(String roleClaimName) {
		this.roleClaimName = roleClaimName;
	}

	public String getTokenHeader() {
		return tokenHeader;
	}

	public void setTokenHeader(String tokenHeader) {
		this.tokenHeader = tokenHeader;
	}
}
