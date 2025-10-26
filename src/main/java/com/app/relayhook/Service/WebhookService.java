package com.app.relayhook.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.app.relayhook.Configs.RabbitMqConfig;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Repository.WorkflowExecutionRepository;
import com.app.relayhook.Repository.WorkflowNodeExecutionRepository;
import com.app.relayhook.Repository.WorkflowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutorService workflowExecutorService;
    private final RabbitTemplate rabbitTemplate;

    public void createExecutionOfWorkflow(long workflowId, Map<String, Long> requestData) {
        Map<String, Long> payload = new HashMap<>();
        payload.put("workflowNodeId", 1L);
        payload.put("workflowExecutionNodeId", 3L);
        log.info("Webhook hit");
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXECUTE_EXCHANGE,
                RabbitMqConfig.EXECUTE_ROUTING_KEY,
                requestData);
        // Optional<Workflow> workflowOpt = workflowRepository.findById(workflowId);
        // if(workflowOpt.isEmpty()){
        // return;
        // }
        // workflowExecutorService.executeAndPersistWorkflow(workflowOpt.get(),
        // requestData);
    }

}
