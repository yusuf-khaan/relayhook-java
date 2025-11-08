package com.app.relayhook.Controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.app.relayhook.DTO.WorkflowDTO;
import com.app.relayhook.Models.SystemIntegrations;
import com.app.relayhook.Models.Users;
import com.app.relayhook.Service.MainService;

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

    @PostMapping("/create-user")
    public Users createUser(HttpServletRequest request, HttpServletResponse response,  @RequestBody Users userDTO) {
        return mainService.createUser(userDTO, response, request);
    } 
}
