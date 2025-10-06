package com.cdc.ots_image_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.cdc.ots_image_service.security.userdetails.CustomUserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        logger.trace("Entering JWT auth filter for request: {}", request.getRequestURI());
        // If the request is an OPTIONS request, pass it through without any processing.
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            logger.trace("OPTIONS request. Bypassing JWT filter.");
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.trace("No 'Bearer ' token found in Authorization header. Passing to next filter.");
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String email = null;
        try {
            email = jwtService.extractEmail(jwt);
        } catch (Exception e) {
            logger.warn("Failed to extract email from JWT. Token may be invalid or malformed.", e);
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            logger.debug("Extracted email '{}' from JWT. SecurityContext is empty.", email);
            // Create UserDetails on-the-fly without a database call
            UserDetails userDetails = new CustomUserDetails(email);

            // Validate token (signature is implicitly validated by extraction)
            if (!jwtService.isTokenExpired(jwt)) {
                logger.debug("Token for '{}' is valid. Setting authentication in SecurityContext.", email);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                logger.warn("Token for '{}' has expired.", email);
            }
        }
        filterChain.doFilter(request, response);
    }
}
