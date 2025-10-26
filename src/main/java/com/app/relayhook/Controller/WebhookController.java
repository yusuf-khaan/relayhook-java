package com.app.relayhook.Controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Repository.WorkflowRepository;
import com.app.relayhook.Service.WebhookService;
import com.app.relayhook.Service.WorkflowExecutorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class WebhookController {

    private final WebhookService webhookService;
    private final WorkflowExecutorService workflowExecutorService;
    private final WorkflowRepository workflowRepository;

    @PostMapping("hook/{workflowId}")
    public Object createExecutionOfWorkflow(@RequestBody Map<String, Object> request, @PathVariable Long workflowId) {
        // webhookService.createExecutionOfWorkflow(workflowId, request);
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow not found"));
        return workflowExecutorService.executeAndPersistWorkflow(workflow, request);
    }

}
