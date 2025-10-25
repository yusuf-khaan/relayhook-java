package com.app.relayhook.Controller;

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
import com.app.relayhook.Service.MainService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RequestMapping("/api/v1")
@RestController
@CrossOrigin(origins = { "http://localhost:4200" }, allowCredentials = "true")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    @PostMapping("/save-workflow")
    public Object saveWorkflow(HttpServletRequest request, @RequestBody WorkflowDTO workflowDTO) {
        return workflowDTO;
        // log.info("save workflow init");
        // return mainService.saveWorkflow(workflowDTO);
    }

    @GetMapping("get-active-integrations")
    public Object getAllIntegrations(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return mainService.getAllIntegrations(pageable, search);
    }
}
