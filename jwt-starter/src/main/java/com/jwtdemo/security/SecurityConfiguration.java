package com.jwtdemo.security;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

	AuthenticationProvider jwtBasedAuthentication() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException {
		Key key = JwtTokenValidator.buildKey(jwtConfiguration.getKey(), jwtConfiguration.getKeyAlgorithm());
		return new JwtAuthenticationProvider(new JwtTokenValidator(key, jwtConfiguration.getRequireAudience(), 
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