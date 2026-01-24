package com.app.relayhook.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.relayhook.DTO.IntegrationsDTO;
import com.app.relayhook.DTO.WorkflowDTO;
import com.app.relayhook.DTO.WorkflowRequestsDTO;
import com.app.relayhook.DTO.WorkflowResponseDTO;
import com.app.relayhook.Models.ScheduleChanges;
import com.app.relayhook.Models.UserIntegrationsCredentials;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Models.WorkflowExecution;
import com.app.relayhook.Service.MainService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        Long userId = (Long) request.getAttribute("userId");
        return mainService.saveWorkflow(userId, workflowDTO);
    }

    @PostMapping("/update-workflow/{id}")
    public Workflow updateWorkflow(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        return mainService.updateWorkflow(id, updates);
    }

    @GetMapping("/get-user-workflows")
    public Page<WorkflowResponseDTO> getUserWorkflows(HttpServletRequest request,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return mainService.getUserWorkflows(request, search, pageable);
    }

    @GetMapping("/get-workflow-analysis/{id}")
    public Map<String, Object> getWorkflowAnalysis(HttpServletRequest request,
            @PathVariable long id) {
        Long userId = (long) request.getAttribute("userId");
        return mainService.getWorkflowLifetimeAnalysis(id, userId);
    }

    @GetMapping("/get-active-integrations")
    public Object getAllIntegrations(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return mainService.getAllIntegrations(pageable, search);
    }

    @GetMapping("/get-integrations")
    public Page<IntegrationsDTO> getIntegrations(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return mainService.getIntegrations(pageable, search);
    }

    @PostMapping("/save-workflow-request")
    public Object saveWorkflowRequests(HttpServletRequest request,
            @RequestBody WorkflowRequestsDTO workflowRequestsDTO) {
        Long userId = (Long) request.getAttribute("userId");
        return mainService.saveWorkflowRequest(workflowRequestsDTO, userId);
    }

    @GetMapping("/get-workflow-requests")
    public Page<WorkflowRequestsDTO> getWorkflowRequest(HttpServletRequest request,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return mainService.getWorkflowRequest(request, search, pageable);
    }

    @GetMapping("/get-scheduled-changes")
    public Page<ScheduleChanges> getScheduledChanges(HttpServletRequest request,
            @RequestParam(required = false) String search,
            @RequestParam(required = true) Long id,
            @PageableDefault(size = 10) Pageable pageable) {
        return mainService.getScheduledChanges(pageable, search, (Long) request.getAttribute("userId"), id);
    }

    @PostMapping("/save-scheduled-changes-request")
    public ScheduleChanges saveScheduledChangesRequest(HttpServletRequest request,
            @RequestBody Map<String, Object> map) {
        Long userId = (Long) request.getAttribute("userId");
        return mainService.saveScheduleChanges(map, userId);
    }

    @PostMapping("/save-user-integration")
    public UserIntegrationsCredentials saveUserIntegration(HttpServletRequest request,
            @RequestBody Map<String, Object> map) {
        Long userId = (Long) request.getAttribute("userId");
        return mainService.saveUserIntegration(map, userId);
    }

    @PostMapping("/update-user-details")
    public Map<String, String> updateUserDetails(HttpServletRequest request, @RequestBody Map<String, String> map) {
        Long userId = (Long) request.getAttribute("userId");
        return mainService.updateUserDetails(map, userId);
    }

    @GetMapping("/send-new-password")
    public Map<String, String> SendNewPassword(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return mainService.sendNewPassword(userId);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request, HttpServletResponse response) {
        Long userId = (Long) request.getAttribute("userId");
        return mainService.me(userId);
    }

    @GetMapping("/workflow-details/{id}")
    public Map<String, Object> getWorkflowStatistics(
            HttpServletRequest request,
            @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return mainService.getWorkflowStatistics(userId, id);
    }

    @GetMapping("/executed-workflow-details/{id}")
    public Page<WorkflowExecution> getWorkflowExecution(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = (Long) request.getAttribute("userId");
        return mainService.getWorkflowExecution(userId, id, page, size);
    }

    @GetMapping("/executed-workflow-nodes/{id}")
    public Object getExecutedWorkflowNodes(HttpServletRequest request, @PathVariable long id){
        long userId = (long) request.getAttribute("userId");
        return mainService.getExecutedWorkflowNodes(userId, id);
    }

    @GetMapping("/get-workflow/{id}")
    public Object getWorkflow(HttpServletRequest request, @PathVariable long id){
        long userId = (long) request.getAttribute("userId");
        return mainService.getWorkflow(userId, id);
    }
}
