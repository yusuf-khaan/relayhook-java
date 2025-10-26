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
    private final WorkflowNodeRepository nodeRepo;
    private final RabbitTemplate rabbitTemplate;
    private final RelayhookAbs relayhookAbs;
    private final ObjectMapper objectMapper;
    private String action = null;
    private String provider = null;

    @RabbitListener(queues = RabbitMqConfig.EXECUTE_QUEUE)
    public void processNode(Map<String, Long> workflowNodesDetail) {
        Long nodeExecutionId = workflowNodesDetail.get("workflowExecutionNodeId");
        NodeErrorLogger.logError("Received execution request for nodeExecutionId: " + nodeExecutionId);

        WorkflowNodeExecution nodeExecution = markNodeRunning(nodeExecutionId);
        Map<String, Object> outputData = new HashMap<>();

        try {
            WorkflowNodes node = fetchNodeEager(nodeExecution.getWorkflowNodeId());
            NodeErrorLogger.logError("Executing node ID " + nodeExecution.getId() + " (Action: " + action + ", Provider: " + provider + ") with input: " + nodeExecution.getInputData());

            outputData = executeNode(node, nodeExecution.getInputData());

            NodeErrorLogger.logError("Node ID " + nodeExecution.getId() + " execution output: " + outputData);

            markNodeCompleted(nodeExecution, outputData);

            workflowNodeExecutionRepository.flush();
            scheduleNextNodes(nodeExecution);

        } catch (Exception e) {
            markNodeFailed(nodeExecution, outputData, e);
        }
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
    protected void markNodeCompleted(WorkflowNodeExecution nodeExecution, Map<String, Object> outputData) {
        nodeExecution.setStatus(NodeStatus.COMPLETED);
        nodeExecution.setOutputData(outputData);
        workflowNodeExecutionRepository.saveAndFlush(nodeExecution);

        NodeErrorLogger.logError("Marked nodeExecutionId " + nodeExecution.getId() + " as COMPLETED");
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
                e.getMessage()
        );
        NodeErrorLogger.logError(errorMsg, e);
        log.error(errorMsg, e);
    }

    @Transactional(readOnly = true)
    protected WorkflowNodes fetchNodeEager(Long nodeId) {
        WorkflowNodes node = nodeRepo.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("WorkflowNode not found with id: " + nodeId));

        Map<String, Object> object = (Map<String, Object>) node.getNodeData().get("object");
        if (object != null) {
            this.action = (String) object.get("action");
            this.provider = (String) object.get("name");
        }

        NodeErrorLogger.logError("Fetched node metadata for nodeId " + nodeId + " (Action: " + action + ", Provider: " + provider + ")");
        return node;
    }

    private Map<String, Object> executeNode(WorkflowNodes node, Map<String, Object> inputData) {
        JsonNode json = relayhookAbs.executeAutomationRequest(inputData, action, provider);
        return objectMapper.convertValue(json, new TypeReference<Map<String, Object>>() {});
    }

   private void scheduleNextNodes(WorkflowNodeExecution completedNode) {
    WorkflowExecution execution = completedNode.getWorkflowExecution();
    List<WorkflowNodeExecution> allNodes = workflowNodeExecutionRepository.findByWorkflowExecution(execution);

    // Build a map of nodeId -> WorkflowNodes
    Map<Long, WorkflowNodes> nodeMetaMap = new HashMap<>();
    for (WorkflowNodeExecution ne : allNodes) {
        nodeMetaMap.put(ne.getWorkflowNodeId(), nodeRepo.findById(ne.getWorkflowNodeId()).orElseThrow());
    }

    // Group nodes by level (use Long as key to avoid type mismatch)
    Map<Long, List<WorkflowNodeExecution>> levelMap = new HashMap<>();
    for (WorkflowNodeExecution ne : allNodes) {
        Long level = nodeMetaMap.get(ne.getWorkflowNodeId()).getLevel();
        levelMap.computeIfAbsent(level, k -> new ArrayList<>()).add(ne);
    }

    NodeErrorLogger.logError("Level map keys: " + levelMap.keySet());
    NodeErrorLogger.logError("Completed node: " + completedNode.getId() + ", level: " + completedNode.getLevel());

    // Determine next level
    Long completedLevel = nodeMetaMap.get(completedNode.getWorkflowNodeId()).getLevel();
    Long nextLevel = completedLevel + 1;
    NodeErrorLogger.logError("Next level to schedule: " + nextLevel);

    List<WorkflowNodeExecution> nextLevelNodes = levelMap.get(nextLevel);
    if (nextLevelNodes == null || nextLevelNodes.isEmpty()) {
        NodeErrorLogger.logError("No nodes found for next level " + nextLevel + " after nodeExecutionId " + completedNode.getId());
        return;
    }

    // Determine if nodes at this level can run in parallel
    boolean canParallel = nextLevelNodes.get(0).getCanExecuteParallel();

    for (WorkflowNodeExecution nextNodeExecution : nextLevelNodes) {
        WorkflowNodes nextNodeMeta = nodeMetaMap.get(nextNodeExecution.getWorkflowNodeId());

        // Check if all input nodes are completed
        boolean ready = true;
        for (Long inputNodeId : nextNodeMeta.getInputNodes()) {
            WorkflowNodeExecution inputExecution = workflowNodeExecutionRepository
                    .findByWorkflowExecutionAndWorkflowNodeId(execution, inputNodeId)
                    .orElseThrow();
            if (inputExecution.getStatus() != NodeStatus.COMPLETED) {
                ready = false;
                NodeErrorLogger.logError("Node " + nextNodeExecution.getId() + " is waiting for input node " + inputNodeId + " to complete");
                break;
            }
        }

        // Queue node if ready and pending
        if (ready && nextNodeExecution.getStatus() == NodeStatus.PENDING) {
            NodeErrorLogger.logError("Queueing node " + nextNodeExecution.getId() + " for execution");
            Map<String, Long> message = Map.of("workflowExecutionNodeId", nextNodeExecution.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXECUTE_EXCHANGE,
                    RabbitMqConfig.EXECUTE_ROUTING_KEY,
                    message
            );

            // If sequential, queue only one node at a time
            if (!canParallel) break;
        } else {
            NodeErrorLogger.logError("Node " + nextNodeExecution.getId() + " is not ready or not pending");
        }
    }
}


}
