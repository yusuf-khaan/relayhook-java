package com.app.relayhook.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@RequiredArgsConstructor
public class WorkflowExecutorService {

    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WorkflowNodeExecutionRepository workflowNodeExecutionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final WorkflowRepository workflowRepo;
    private final WorkflowNodeRepository nodeRepo;

    @Transactional
    public void executeAndPersistWorkflow(Workflow workflow, Map<String, Object> requestData) {

        WorkflowExecution workflowExecution = new WorkflowExecution();
        workflowExecution.setWorkflowId(workflow.getId());
        workflowExecution.setTrigger(workflow.getTrigger());
        workflowExecution = workflowExecutionRepository.save(workflowExecution);

        List<WorkflowNodeExecution> nodeExecutions = new ArrayList<>();
        for (WorkflowNodes node : workflow.getNodes()) {
            WorkflowNodeExecution nodeExecution = new WorkflowNodeExecution();
            nodeExecution.setWorkflowExecution(workflowExecution);
            nodeExecution.setWorkflowNodeId(node.getId());
            nodeExecution.setStatus(NodeStatus.PENDING);
            nodeExecution.setRetriesLeft(3L);
            nodeExecution.setInputData(requestData != null ? requestData : Map.of());
            nodeExecution.setOutputData(Map.of());
            nodeExecution.setErrorLogs(List.of());
            nodeExecutions.add(nodeExecution);
        }
        workflowNodeExecutionRepository.saveAll(nodeExecutions);
        workflowExecution.setWorkflowExecutionNodes(nodeExecutions);


        nodeExecutions.stream()
                .filter(ne -> nodeRepo.findById(ne.getWorkflowNodeId())
                        .orElseThrow()
                        .getInputNodes().isEmpty())
                .forEach(ne -> rabbitTemplate.convertAndSend(
                        RabbitMqConfig.EXCHANGE_NAME,
                        RabbitMqConfig.ROUTING_KEY,
                        ne.getId()
                ));
    }
}
