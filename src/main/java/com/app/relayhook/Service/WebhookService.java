package com.app.relayhook.Service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Repository.WorkflowExecutionRepository;
import com.app.relayhook.Repository.WorkflowNodeExecutionRepository;
import com.app.relayhook.Repository.WorkflowRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutorService workflowExecutorService;

    public void createExecutionOfWorkflow(long workflowId, Map<String, Object> requestData){
        Optional<Workflow> workflowOpt = workflowRepository.findById(workflowId);
        if(workflowOpt.isEmpty()){
            return;
        }
        workflowExecutorService.executeAndPersistWorkflow(workflowOpt.get(), requestData);
    }

}
