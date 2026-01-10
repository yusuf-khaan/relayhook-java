package com.app.relayhook.Controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.app.relayhook.DTO.WorkflowDTO;
import com.app.relayhook.Models.SystemIntegrations;
import com.app.relayhook.Models.Users;
import com.app.relayhook.Service.MainService;
import com.app.relayhook.Service.UserService;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RequestMapping("/auth")
@RestController
@CrossOrigin(origins = { "http://localhost:4200" }, allowCredentials = "true")
@RequiredArgsConstructor
public class UserController {

    private final MainService mainService;
    private final UserService userService;
    private final JavaMailSender mailSender;

    @PostMapping("/register")
    public Users createUser(HttpServletRequest request, HttpServletResponse response,
            @RequestBody Map<String, String> userDTO) {
        return userService.createUser(userDTO, response);
    }

    @PostMapping("/login")
    public Map<String, String> loginUser(HttpServletRequest request, HttpServletResponse response,
            @RequestBody Map<String, String> loginDTO) {
        return userService.loginUser(loginDTO, response, request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }
}
