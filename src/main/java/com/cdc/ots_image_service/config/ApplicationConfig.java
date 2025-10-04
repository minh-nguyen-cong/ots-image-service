package com.cdc.ots_image_service.config;

import com.cdc.ots_image_service.security.userdetails.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

@Configuration
public class ApplicationConfig {

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // This provider's main job in a stateless resource server is to take the
        // principal (email) from the token processed by JwtAuthFilter and package it
        // into a fully authenticated UserDetails object for the security context.
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        // We provide a simple UserDetailsService that creates a UserDetails object
        // directly from the username (email) extracted from the JWT.
        // No database call is needed because the JWT's validity is our proof of authentication.
        authProvider.setUserDetailsService(username -> new CustomUserDetails(username));

        // A password encoder is not needed since we are not verifying passwords.
        return authProvider;
    }
}