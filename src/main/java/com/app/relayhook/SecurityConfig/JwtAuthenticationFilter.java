package com.app.relayhook.SecurityConfig;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.relayhook.Models.Users;
import com.app.relayhook.Service.CustomUserDetailService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Cookies cookies;
    private final JwtUtil jwtService;
    private final CustomUserDetailService customUserDetailService;

    @Autowired
    public JwtAuthenticationFilter(Cookies cookies, JwtUtil jwtService,
            CustomUserDetailService customUserDetailService) {
        this.cookies = cookies;
        this.jwtService = jwtService;
        this.customUserDetailService = customUserDetailService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("doFilterInternal running for {}", request.getRequestURI());
        // Get JWT from Cookie
        String jwt = cookies.getJwtFromCookie(request);
        // log.info(jwt + "this is jwt");
        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extractEmail(jwt);

            if (email != null) {
                UserDetails userDetails = customUserDetailService.loadUserByUsername(email);

                if (jwtService.validateToken(jwt, userDetails)) {
                    authenticateUser(jwt, request);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.info("Path: {}", path);

        // Exclude paths that should not trigger JWT authentication
        boolean skip = path.startsWith("/auth/") ||
                path.startsWith("/oauth2") ||
                path.startsWith("/login") ||
                path.startsWith("/go") ||
                path.startsWith("/actuator/health") ||
                // path.startsWith("/api/saveAndCheckSlug") ||
                path.startsWith("/error") ||
                path.startsWith("/api/form/") ||
                path.startsWith("/api/saveResponse/") ||
                path.startsWith("/api/public/") || // these are for public path free from original cookie auth flow
                path.startsWith("/webhooks/") ||
                path.startsWith("/api/caser/");

        log.info("shouldNotFilter = {}", skip);
        return skip;
    }

    private void authenticateUser(String jwt, HttpServletRequest request) {
        log.info("Authenticating JWT for {}", request.getRequestURI());
        String email = jwtService.extractEmail(jwt);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = customUserDetailService.loadUserByUsername(email);

            if (jwtService.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in the security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                Long surgeId = ((Users) userDetails).getId();
                request.setAttribute("surgeId", surgeId);
            }
        }
    }
}