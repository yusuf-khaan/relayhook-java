package com.app.relayhook.RabbitListeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.relayhook.Configs.RabbitMqConfig;
import com.app.relayhook.Enums.NodeStatus;
import com.app.relayhook.Integrations.Relayhook.RelayhookAbs;
import com.app.relayhook.Logs.NodeErrorLogger;
import com.app.relayhook.Models.WorkflowExecution;
import com.app.relayhook.Models.WorkflowNodeExecution;
import com.app.relayhook.Models.WorkflowNodes;
import com.app.relayhook.Repository.WorkflowExecutionRepository;
import com.app.relayhook.Repository.WorkflowNodeExecutionRepository;
import com.app.relayhook.Repository.WorkflowNodeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqListener {

    private final WorkflowNodeExecutionRepository workflowNodeExecutionRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RelayhookAbs relayhookAbs;
    private final ObjectMapper objectMapper;
    private final WorkflowNodeRepository workflowNodeRepository;
    private String action = null;
    private String provider = null;

    @RabbitListener(queues = RabbitMqConfig.EXECUTE_QUEUE)
    public void processNode(Map<String, Object> workflowNodesDetail) {
        Number workflowNodeIdNum = (Number) workflowNodesDetail.get("OriginalworkflowNodeId");
        Long orginalWorkflowId = workflowNodeIdNum.longValue();

        Number workflowExecutionIdNum = (Number) workflowNodesDetail.get("workflowExecutionId");
        Long workflowExecutionId = workflowExecutionIdNum.longValue();

        Map<String, Object> testData = objectMapper.convertValue(workflowNodesDetail.get("inputData"),
                new TypeReference<Map<String, Object>>() {
                });
                                Map<String, Object> requestData = new HashMap<>();
        testData.put("to", "khanyusuf0966@gmail.com");
        testData.put("subject", "Welcome to RelayHooks!");
        testData.put("message", "Hello there 👋, we're glad to have you onboard.");
        testData.put("rawHtml", "<h2>Welcome to <b>RelayHooks</b>!</h2><p>We’re glad to have you. Start exploring your integrations today 🚀</p>");

        WorkflowNodes workflowNode = workflowNodeRepository.findById(orginalWorkflowId).orElseThrow();
        WorkflowExecution workflowExecution = workflowExecutionRepository.findById(workflowExecutionId).orElseThrow();
        WorkflowNodeExecution workflowNodeExecution = createExecutionWorkflowNode(workflowExecution, workflowNode,
                requestData);
        workflowNodeExecution = markNodeRunning(workflowNodeExecution.getId());
        Map<String, Object> outputData = new HashMap<>();
        Map<String, Object> object = objectMapper.convertValue(workflowNode.getNodeData().get("object"),
                new TypeReference<Map<String, Object>>() {
                });
        NodeErrorLogger.logError("workflowNode 123458"+object);
        this.action = object.get("action").toString();
        this.provider = object.get("name").toString();

        try {
            NodeErrorLogger.logError("Executing node ID " + workflowNodeExecution.getId() + " (Action: " + action
                    + ", Provider: " + provider + ") with input: " + workflowNodeExecution.getInputData());
            outputData = executeNode(workflowNode, requestData);
            workflowNodeExecution.setOutputData(outputData);
            NodeErrorLogger.logError("Node ID " + workflowNodeExecution.getId());
            workflowNodeExecution = markNodeCompleted(workflowNodeExecution, outputData);
            workflowNodeExecutionRepository.flush();
            scheduleNextNodes(workflowNode, workflowNodeExecution);
        } catch (Exception e) {
            markNodeFailed(workflowNodeExecution, outputData, e);
        }
    }

    @Transactional
    public WorkflowNodeExecution createExecutionWorkflowNode(WorkflowExecution workflowExecution,
            WorkflowNodes workflowNode, Map<String, Object> inputData) {
        WorkflowNodeExecution nodeExecution = new WorkflowNodeExecution();
        nodeExecution.setWorkflowExecution(workflowExecution);

        // Reference the original workflow node
        nodeExecution.setWorkflowNodeId(workflowNode.getId());
        nodeExecution.setLevel(workflowNode.getLevel());
        nodeExecution.setCanExecuteParallel(workflowNode.getCanExecuteParallel());
        nodeExecution.setStatus(NodeStatus.PENDING);
        nodeExecution.setRetriesLeft(3L);
        nodeExecution.setInputData(inputData != null ? inputData : Map.of());
        nodeExecution.setOutputData(Map.of());
        nodeExecution.setErrorLogs(new ArrayList<>());
        workflowNodeExecutionRepository.saveAndFlush(nodeExecution);
        return nodeExecution;
    }

    @Transactional
    protected WorkflowNodeExecution markNodeRunning(Long nodeExecutionId) {
        WorkflowNodeExecution nodeExecution = workflowNodeExecutionRepository.findById(nodeExecutionId)
                .orElseThrow(() -> new RuntimeException("NodeExecution not found with id: " + nodeExecutionId));

        nodeExecution.setStatus(NodeStatus.RUNNING);
        NodeErrorLogger.logError("Marked nodeExecutionId " + nodeExecutionId + " as RUNNING");
        return workflowNodeExecutionRepository.save(nodeExecution);
    }

    @Transactional
    protected WorkflowNodeExecution markNodeCompleted(WorkflowNodeExecution nodeExecution,
            Map<String, Object> outputData) {
        nodeExecution.setStatus(NodeStatus.COMPLETED);
        nodeExecution.setOutputData(outputData);
        NodeErrorLogger.logError("Marked nodeExecutionId " + nodeExecution.getId() + " as COMPLETED");
        return workflowNodeExecutionRepository.saveAndFlush(nodeExecution);
    }

    @Transactional
    protected void markNodeFailed(WorkflowNodeExecution nodeExecution, Map<String, Object> outputData, Exception e) {
        nodeExecution.setStatus(NodeStatus.FAILED);
        nodeExecution.setOutputData(outputData);
        workflowNodeExecutionRepository.saveAndFlush(nodeExecution);

        String errorMsg = String.format(
                "NodeExecution ID %d failed for Node ID %d. Exception: %s",
                nodeExecution.getId(),
                nodeExecution.getWorkflowNodeId(),
                e.getMessage());
        NodeErrorLogger.logError(errorMsg, e);
        log.error(errorMsg, e);
    }

    private Map<String, Object> executeNode(WorkflowNodes workflowNode, Map<String, Object> inputData) {
        JsonNode json = relayhookAbs.executeAutomationRequest(inputData, action, provider);
        return objectMapper.convertValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    @Transactional
    private void scheduleNextNodes(WorkflowNodes completedNode, WorkflowNodeExecution completedExecution) {
        Long workflowId = completedNode.getWorkflow().getId();

        // Find all nodes that depend on the completed one
        List<WorkflowNodes> dependentNodes = workflowNodeRepository
                .findByWorkflowIdAndInputNodesContaining(completedNode.getWorkflow().getId(),
                        completedNode.getNodeId());

        if (dependentNodes.isEmpty()) {
            NodeErrorLogger.logError("No dependent nodes found for nodeId " + completedNode.getNodeId());
            return;
        }

        // dependent node are next nodes
        // For each dependent node, check if *all* its inputs are completed
        for (WorkflowNodes WorkflowNode : dependentNodes) {
            boolean allInputsDone = true;
            if (WorkflowNode.getInputNodes() != null && !WorkflowNode.getInputNodes().isEmpty()) {
                for (Long inputNodeId : WorkflowNode.getInputNodes()) {
                    WorkflowNodes inputNode = workflowNodeRepository.findByNodeIdAndWorkflowId(inputNodeId, workflowId)
                            .orElse(null);
                    if (inputNode == null) {
                        allInputsDone = false;
                        break;
                    }

                    // Fetch execution record for that input node
                    WorkflowNodeExecution exec = workflowNodeExecutionRepository
                            .findTopByWorkflowNodeIdAndWorkflowExecutionIdOrderByIdDesc(
                                    inputNode.getId(),
                                    completedExecution.getWorkflowExecution().getId());

                    if (exec == null || exec.getStatus() != NodeStatus.COMPLETED) {
                        allInputsDone = false;
                        break;
                    }
                }
            }

            // If all inputs done, push next node to RabbitMQ
            if (allInputsDone) {
                NodeErrorLogger
                        .logError("All inputs done for nodeId " + WorkflowNode.getNodeId() + ". Scheduling execution.");
                Map<String, Object> payload = new HashMap<>();
                payload.put("OriginalworkflowNodeId", WorkflowNode.getId());
                payload.put("workflowExecutionId", completedExecution.getWorkflowExecution().getId());
                payload.put("inputData", completedExecution.getOutputData());
                NodeErrorLogger.logError("1238189 "+payload);
                rabbitTemplate.convertAndSend(RabbitMqConfig.EXECUTE_QUEUE, payload);
            } else {
                NodeErrorLogger
                        .logError("Not all inputs done for nodeId " + WorkflowNode.getNodeId() + ", skipping for now.");
            }
        }
    }

}
