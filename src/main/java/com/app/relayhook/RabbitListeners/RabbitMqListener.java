package com.app.relayhook.RabbitListeners;

import java.time.Instant;
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
import com.app.relayhook.Models.WorkflowNodes.SchemaData;
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
    private Long workflowId = null;
    private Integer workFlowNodeId = null;
    private Integer nodeComesFrom = null;
    private Long workflowExecutionId = null;

    @RabbitListener(queues = RabbitMqConfig.EXECUTE_QUEUE)
    public void processNode(Map<String, Object> workflowNodesDetail) {
        Number workflowNodeIdNum = (Number) workflowNodesDetail.get("OriginalworkflowNodeId");
        this.workFlowNodeId = workflowNodeIdNum.intValue();
        Number workflowIdNum = (Number) workflowNodesDetail.get("workflowId");
        this.workflowId = workflowIdNum.longValue();
        Number nodeComesFrom = (Number) workflowNodesDetail.get("nodeComesFrom");
        this.nodeComesFrom = nodeComesFrom != null ? nodeComesFrom.intValue() : null;

        Number workflowExecutionIdNum = (Number) workflowNodesDetail.get("workflowExecutionId");
        Long workflowExecutionId = workflowExecutionIdNum.longValue();
        this.workflowExecutionId = workflowExecutionId;

        Map<String, Object> requestData = objectMapper.convertValue(workflowNodesDetail.get("inputData"),
                new TypeReference<Map<String, Object>>() {
                });
        WorkflowExecution workflowExecution = workflowExecutionRepository.findById(workflowExecutionId).orElseThrow();
        WorkflowNodes workflowNode = workflowNodeRepository
                .findByNodeIdAndWorkflowId(this.workFlowNodeId, this.workflowId)
                .orElseThrow(() -> new RuntimeException(
                        "No node found for nodeId: " + this.workFlowNodeId +
                                " in workflowId: " + workflowExecution.getWorkflow().getId()));
        List<SchemaData> schemaData = workflowNode.getSchemaData().getOrDefault(this.nodeComesFrom, null);
        NodeErrorLogger.logError("69 " + String.valueOf(schemaData));

        WorkflowNodeExecution workflowNodeExecution = createExecutionWorkflowNode(workflowExecution, workflowNode,
                requestData);
        workflowNodeExecution = markNodeRunning(workflowNodeExecution.getId());
        Map<String, Object> outputData = new HashMap<>();

        Map<String, Object> object = objectMapper.convertValue(workflowNode.getNodeData().get("object"),
                new TypeReference<Map<String, Object>>() {
                });
        this.action = object.get("action").toString();
        this.provider = object.get("provider").toString();

        try {
            if (workflowNode.getNodeData().getOrDefault("isInitial", false) == Boolean.FALSE) {
                NodeErrorLogger.logError("schemaData 86 " + schemaData);
                NodeErrorLogger.logError("requestData 87" + requestData);
                NodeErrorLogger.logError("provider 87" + object.get("provider").toString());
                outputData = executeNode(workflowNode, requestData, schemaData);
                NodeErrorLogger.logError("outputData 79 is " + outputData);
            } else if (workflowNode.getNodeData().getOrDefault("isInitial", false) == Boolean.TRUE) { // skip the start
                                                                                                      // node
                outputData = requestData;
            }
            Integer status = (Integer) outputData.getOrDefault("status", 200);
            if (status >= 400) {
                NodeErrorLogger.logError("failed node");
                markNodeFailed(
                        workflowNodeExecution,
                        outputData,
                        requestData,
                        schemaData,
                        workflowNode,
                        workflowExecutionId);
                return;
            }
            NodeErrorLogger.logError("never print");
            workflowNodeExecution.setOutputData(outputData);
            workflowNodeExecution = markNodeCompleted(workflowNodeExecution, outputData, workflowNode);
            workflowNodeExecutionRepository.flush();
            scheduleNextNodes(workflowNode, workflowNodeExecution);
        } catch (Exception e) {
            markNodeFailed(workflowNodeExecution, outputData, requestData, schemaData, workflowNode,
                    workflowExecutionId);
        }
    }

    @Transactional
    public WorkflowNodeExecution createExecutionWorkflowNode(WorkflowExecution workflowExecution,
            WorkflowNodes workflowNode, Map<String, Object> inputData) {
        WorkflowNodeExecution nodeExecution = new WorkflowNodeExecution();

        String provider = workflowNode.getProvider();
        if (provider == null || provider.trim() == "") {
            Map<String, Object> nodeData = workflowNode.getNodeData();
            JsonNode nodeDataJson = objectMapper.valueToTree(nodeData);
            provider = nodeDataJson.path("object").path("provider").asText(null);
            workflowNode.setProvider(provider);
            workflowNodeRepository.save(workflowNode);
        }

        nodeExecution.setWorkflowExecution(workflowExecution);

        // Reference the original workflow node
        nodeExecution.setWorkflowNodeId(workflowNode.getId());
        nodeExecution.setLevel(workflowNode.getLevel());
        nodeExecution.setCanExecuteParallel(workflowNode.getCanExecuteParallel());
        nodeExecution.setStatus(NodeStatus.PENDING);
        nodeExecution.setRetriesLeft(3);
        nodeExecution.setInputData(inputData != null ? inputData : Map.of());
        nodeExecution.setOutputData(Map.of());
        nodeExecution.setErrorLogs(new ArrayList<>());
        nodeExecution.setProvider(provider);
        workflowNodeExecutionRepository.saveAndFlush(nodeExecution);
        return nodeExecution;
    }

    @Transactional
    protected WorkflowNodeExecution markNodeRunning(Long nodeExecutionId) {
        WorkflowNodeExecution nodeExecution = workflowNodeExecutionRepository.findById(nodeExecutionId)
                .orElse(null);
        if (nodeExecution == null) {
            return null;
        }
        nodeExecution.setStatus(NodeStatus.RUNNING);
        return workflowNodeExecutionRepository.save(nodeExecution);
    }

    @Transactional
    protected WorkflowNodeExecution markNodeCompleted(WorkflowNodeExecution nodeExecution,
            Map<String, Object> outputData, WorkflowNodes workflowNode) {
        nodeExecution.setStatus(NodeStatus.COMPLETED);
        nodeExecution.setOutputData(outputData);
        return workflowNodeExecutionRepository.saveAndFlush(nodeExecution);
    }

    @Transactional
    protected void markNodeFailed(
            WorkflowNodeExecution nodeExecution,
            Map<String, Object> outputData,
            Map<String, Object> requestData,
            List<SchemaData> schemaData,
            WorkflowNodes workflowNode,
            Long workflowExecutionId) {
        int retriesLeft = nodeExecution.getRetriesLeft() - 1;
        nodeExecution.setRetriesLeft(retriesLeft);
        nodeExecution.setOutputData(outputData);
        String errorMsg = String.format(
                "NodeExecution ID %d failed for Node ID %d",
                nodeExecution.getId(),
                nodeExecution.getWorkflowNodeId());
        NodeErrorLogger.logError(errorMsg);
        // FINAL FAILURE
        if (retriesLeft < 0) {
            nodeExecution.setStatus(NodeStatus.FAILED_FINAL); // or FAILED
            workflowNodeExecutionRepository.saveAndFlush(nodeExecution);
            return;
        }
        // RETRY
        nodeExecution.setStatus(NodeStatus.RETRYING);
        workflowNodeExecutionRepository.saveAndFlush(nodeExecution);
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestData", requestData);
        payload.put("schemaData", schemaData);
        payload.put("workflowNode", workflowNode);
        payload.put("workflowExecutionId", workflowExecutionId);
        payload.put("nodeExecutionId", nodeExecution.getId());
        rabbitTemplate.convertAndSend(RabbitMqConfig.RETRY_QUEUE, payload);
    }

    private Map<String, Object> executeNode(WorkflowNodes workflowNode, Map<String, Object> inputData,
            List<SchemaData> schemaData) {
        JsonNode json = relayhookAbs.executeAutomationRequest(inputData, action, provider, schemaData);
        return objectMapper.convertValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    @Transactional
    private void scheduleNextNodes(WorkflowNodes completedNode, WorkflowNodeExecution completedExecution) {
        // Find all nodes that depend on the completed one
        List<WorkflowNodes> dependentNodes = workflowNodeRepository
                .findByWorkflowIdAndInputNodesContaining(completedNode.getWorkflow().getId(),
                        completedNode.getNodeId());

        if (dependentNodes.isEmpty()) {
            return;
        }

        // dependent node are next nodes
        // For each dependent node, check if *all* its inputs are completed
        for (WorkflowNodes WorkflowNode : dependentNodes) {
            boolean allInputsDone = true;
            if (WorkflowNode.getInputNodes() != null && !WorkflowNode.getInputNodes().isEmpty()) {
                for (Integer inputNodeId : WorkflowNode.getInputNodes()) {
                    WorkflowNodes inputNode = workflowNodeRepository
                            .findByNodeIdAndWorkflowId(inputNodeId, this.workflowId)
                            .orElse(null);

                    // its only a safety back if there is a input node that does not exist
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

            // first input node should always has its execution xomplete at start only
            if (allInputsDone) {
                Boolean checkIfObjectEmpty = checkIfNodeDataObjectIsEmpty(WorkflowNode);
                if (checkIfObjectEmpty) {
                    continue;
                }
                Map<String, Object> payload = new HashMap<>();
                payload.put("OriginalworkflowNodeId", WorkflowNode.getNodeId());
                payload.put("workflowExecutionId", completedExecution.getWorkflowExecution().getId());
                payload.put("inputData", completedExecution.getOutputData());
                payload.put("workflowId", this.workflowId);
                payload.put("nodeComesFrom", completedNode.getNodeId());
                rabbitTemplate.convertAndSend(RabbitMqConfig.EXECUTE_QUEUE, payload);
            } else {
            }
        }
    }

    private Boolean checkIfNodeDataObjectIsEmpty(WorkflowNodes workflowNode) {
        Map<String, Object> nodeDataMap = objectMapper.convertValue(workflowNode.getNodeData(),
                new TypeReference<Map<String, Object>>() {
                });
        if (nodeDataMap.containsKey("object")) {
            Map<String, Object> objectMap = objectMapper.convertValue(nodeDataMap.get("object"),
                    new TypeReference<Map<String, Object>>() {
                    });
            if (objectMap == null || objectMap.isEmpty()) {
                return true;
            }
            String apiUrlInObject = objectMap.getOrDefault("apiUrl", "").toString();
            return apiUrlInObject.isEmpty();
        }
        return false;
    }

    @RabbitListener(queues = RabbitMqConfig.RETRY_QUEUE)
    @Transactional
    public void retryQueue(Map<String, Object> payload) {
        NodeErrorLogger.logError("Retey queueu");
        Long workflowExecutionId = ((Number) payload.get("workflowExecutionId")).longValue();
        Long nodeExecutionId = ((Number) payload.get("nodeExecutionId")).longValue();
        Map<String, Object> requestData = (Map<String, Object>) payload.get("requestData");
        Object schemaDataRaw = payload.get("schemaData");
        Object workflowNodeRaw = payload.get("workflowNode");
        // Convert payload back to proper objects
        List<SchemaData> schemaData = objectMapper.convertValue(
                schemaDataRaw,
                new TypeReference<List<SchemaData>>() {
                });
        WorkflowNodes workflowNode = objectMapper.convertValue(
                workflowNodeRaw,
                WorkflowNodes.class);
        // Load existing execution
        WorkflowNodeExecution execution = workflowNodeExecutionRepository.findById(nodeExecutionId)
                .orElseThrow(() -> new RuntimeException("NodeExecution not found for retry id=" + nodeExecutionId));
        // Check if retries are exhausted
        if (execution.getRetriesLeft() <= 0) {
            execution.setStatus(NodeStatus.FAILED_FINAL);
            workflowNodeExecutionRepository.saveAndFlush(execution);
            log.error("Retry exhausted for nodeExecutionId={}", nodeExecutionId);
            return; // stop retrying
        }
        try {
            // Mark as running and decrement retries
            execution.setStatus(NodeStatus.RUNNING);
            execution.setRetriesLeft(execution.getRetriesLeft() - 1);
            workflowNodeExecutionRepository.saveAndFlush(execution);
            // Execute the node again
            Map<String, Object> outputData = executeNode(workflowNode, requestData, schemaData);
            NodeErrorLogger.logError("response " + outputData);
            Integer status = (Integer) outputData.getOrDefault("status", 200);
            if (status >= 400) {
                // Node failed again → will retry or fail finally
                markNodeFailed(execution, outputData, requestData, schemaData, workflowNode, workflowExecutionId);
                return;
            }
            // Node succeeded → mark completed and schedule downstream nodes
            execution.setOutputData(outputData);
            markNodeCompleted(execution, outputData, workflowNode);
            scheduleNextNodes(workflowNode, execution);
        } catch (Exception e) {
            // Unexpected error → retry again
            markNodeFailed(execution, execution.getOutputData(), requestData, schemaData, workflowNode,
                    workflowExecutionId);
        }
    }

}

/*
 * here inside never use database node id , always use combination of workflowId
 * and workflowNodeId
 */