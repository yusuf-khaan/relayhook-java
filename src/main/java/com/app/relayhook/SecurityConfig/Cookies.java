package com.app.relayhook.SecurityConfig;


import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import com.app.relayhook.Models.Users;
import com.app.relayhook.Repository.UsersRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class Cookies {

    private final JwtUtil jwtUtil;
    private final UsersRepository usersRepository;

    public Cookies(JwtUtil jwtUtil, UsersRepository usersRepository) {
        this.jwtUtil = jwtUtil;
        this.usersRepository = usersRepository;
    }

    public void setCookie(String jwtToken, HttpServletResponse response, HttpServletRequest request) {
        String requestDomain = request.getServerName(); // e.g., "localhost" or "surgeit.co.in"

        String cookieDomain = null;
        if (!requestDomain.equals("localhost") && !requestDomain.startsWith("127.")) {
            cookieDomain = ".relayhook.in"; // ✅ Only apply domain in production
        }

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("jwt", jwtToken)
                .httpOnly(true)
                .secure(true)
                // .sameSite("None")
                .sameSite("None")
                .path("/")
                .maxAge(24 * 60 * 60);

        if (cookieDomain != null) {
            cookieBuilder.domain(cookieDomain);
        }

        // if (!"localhost".equalsIgnoreCase(requestDomain) &&
        // !requestDomain.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
        // cookieBuilder.domain("surgeit.co.in");
        // } later

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    public String getJwtFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, "jwt");
        return cookie != null ? cookie.getValue() : null;
    }

    public Long getUserIdFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, "jwt");
        if (cookie != null) {
            String jwtToken = cookie.getValue();
            try {
                String email = jwtUtil.extractEmail(jwtToken);
                Optional<Users> user = usersRepository.findByEmail(email);

                if (user.isPresent()) {
                    return user.get().getId();
                } else {
                    throw new RuntimeException("User not found");
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid User");
            }
        }
        throw new RuntimeException("Invalid User");
    }

}
