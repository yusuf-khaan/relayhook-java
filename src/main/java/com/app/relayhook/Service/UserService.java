package com.app.relayhook.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.app.relayhook.DTO.WorkflowDTO;
import com.app.relayhook.DTO.WorkflowNodesDTO;
import com.app.relayhook.Integrations.Mail.MailAbs;
import com.app.relayhook.Integrations.Relayhook.RelayhookAbs;
import com.app.relayhook.Logs.NodeErrorLogger;
import com.app.relayhook.Models.SystemIntegrations;
import com.app.relayhook.Models.Users;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Models.WorkflowNodes;
import com.app.relayhook.Models.WorkflowRequests;
import com.app.relayhook.Repository.SystemIntegrationsRepository;
import com.app.relayhook.Repository.UsersRepository;
import com.app.relayhook.Repository.WorkflowRepository;
import com.app.relayhook.Repository.WorkflowRequestsRepository;
import com.app.relayhook.SecurityConfig.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final WorkflowRepository workflowRepository;
    private final SystemIntegrationsRepository systemIntegrationsRepository;
    private final RelayhookAbs relayhookAbs;
    private final ObjectMapper objectMapper;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final WorkflowRequestsRepository workflowRequestsRepository;
    private static final long EXPIRATION_MILLIS = 1000L * 60 * 60 * 24 * 7; // 7 days
    private final MailAbs mailAbs;

    public Users createUser(Map<String, String> userDTO, HttpServletResponse response) {
        Optional<Users> existingUser = usersRepository.findByEmail(userDTO.get("email"));
        if (existingUser.isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }
        Users user = new Users();
        user.setEmail(userDTO.get("email"));
        user.setPassword(passwordEncoder.encode(userDTO.get("password")));
        Users savedUser = usersRepository.save(user);
        String jwtToken = jwtUtil.generateToken(savedUser.getUsername());
        ResponseCookie cookie = ResponseCookie.from("jwt", jwtToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return savedUser;
    }

    public Map<String, String> loginUser(Map<String, String> userDTO, HttpServletResponse response,
            HttpServletRequest request) {
        Users user = usersRepository.findByEmail(userDTO.get("email")).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!passwordEncoder.matches(userDTO.get("password"), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        String jwtToken = jwtUtil.generateToken(userDTO.get("email"));
        // log.info("Generated JWT Token: {}", jwtToken);
        ResponseCookie cookie = ResponseCookie.from("jwt", jwtToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(EXPIRATION_MILLIS / 1000) // Convert milliseconds to seconds
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        // Map<String,Object> map = Map.of(
        //     "name", user.getUsername(),
        //     "loginDate", new SimpleDateFormat("dd-MM-yyyy").format(new Date())
        // );
        // mailAbs.sendTemplateMail("thekhanyusuf096@gmail.com","Welcome to Relayhooks! ", "login-email", map);

        return Map.of("message", "Login successful",
                "navigate", "/hooks/project");
    }
}