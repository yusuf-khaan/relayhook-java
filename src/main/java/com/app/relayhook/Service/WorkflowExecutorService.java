package com.app.relayhook.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.app.relayhook.Configs.RabbitMqConfig;
import com.app.relayhook.Enums.NodeStatus;
import com.app.relayhook.Enums.WorkflowStatus;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Models.WorkflowExecution;
import com.app.relayhook.Models.WorkflowNodeExecution;
import com.app.relayhook.Models.WorkflowNodes;
import com.app.relayhook.Repository.WorkflowExecutionRepository;
import com.app.relayhook.Repository.WorkflowNodeExecutionRepository;
import com.app.relayhook.Repository.WorkflowNodeRepository;
import com.app.relayhook.Repository.WorkflowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutorService {

    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WorkflowNodeExecutionRepository workflowNodeExecutionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final WorkflowRepository workflowRepo;
    private final WorkflowNodeRepository workflowNodeRepository;

    @Transactional
    public Workflow executeAndPersistWorkflow(Workflow workflow, Map<String, Object> requestData) {

        WorkflowExecution workflowExecution = new WorkflowExecution();
        workflowExecution.setWorkflow(workflow);
        workflowExecution.setTrigger(workflow.getTrigger());
        workflowExecution = workflowExecutionRepository.saveAndFlush(workflowExecution);

        for (WorkflowNodes node : workflow.getWorkflowNodesData()) {
            if (node.getInputNodes().isEmpty()) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("OriginalworkflowNodeId", node.getId());
                payload.put("workflowExecutionId", workflowExecution.getId());
                payload.put("inputData", requestData);

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        rabbitTemplate.convertAndSend(
                                RabbitMqConfig.EXECUTE_EXCHANGE,
                                RabbitMqConfig.EXECUTE_ROUTING_KEY,
                                payload);
                        log.info("Enqueued root node to RabbitMQ: {}", payload);
                    }
                });
            }
        }

        return workflow;
    }
}