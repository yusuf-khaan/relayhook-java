package com.app.relayhook.Controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Repository.WorkflowRepository;
import com.app.relayhook.Service.WebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class WebhookController {
    
    private final WebhookService webhookService;

    @PostMapping("hook/{workflowId}")
    public void createExecutionOfWorkflow(@RequestBody Map<String,Object> request ,@PathVariable Long workflowId){
        webhookService.createExecutionOfWorkflow(workflowId, request);
    }

}
