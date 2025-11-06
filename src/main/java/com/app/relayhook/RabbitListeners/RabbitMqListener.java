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
    private Long workflowId = null;
    private Long workFlowNodeId = null;

    @RabbitListener(queues = RabbitMqConfig.EXECUTE_QUEUE)
    public void processNode(Map<String, Object> workflowNodesDetail) {
        Number workflowNodeIdNum = (Number) workflowNodesDetail.get("OriginalworkflowNodeId");
        this.workFlowNodeId = workflowNodeIdNum.longValue();
        Number workflowIdNum = (Number) workflowNodesDetail.get("workflowId");
        this.workflowId = workflowIdNum.longValue();

        Number workflowExecutionIdNum = (Number) workflowNodesDetail.get("workflowExecutionId");
        Long workflowExecutionId = workflowExecutionIdNum.longValue();

        Map<String, Object> requestData = objectMapper.convertValue(workflowNodesDetail.get("inputData"),
                new TypeReference<Map<String, Object>>() {});
        WorkflowExecution workflowExecution = workflowExecutionRepository.findById(workflowExecutionId).orElseThrow();
        WorkflowNodes workflowNode = workflowNodeRepository
                .findByNodeIdAndWorkflowId(this.workFlowNodeId, this.workflowId)
                .orElseThrow(() -> new RuntimeException(
                        "No node found for nodeId: " + this.workFlowNodeId +
                                " in workflowId: " + workflowExecution.getWorkflow().getId()));
        
        WorkflowNodeExecution workflowNodeExecution = createExecutionWorkflowNode(workflowExecution, workflowNode,
                requestData);
        workflowNodeExecution = markNodeRunning(workflowNodeExecution.getId());
        Map<String, Object> outputData = new HashMap<>();
        Map<String, Object> object = objectMapper.convertValue(workflowNode.getNodeData().get("object"),
                new TypeReference<Map<String, Object>>() {
                });
        this.action = object.get("action").toString();
        this.provider = object.get("name").toString();

        try {
            // NodeErrorLogger.logError("OriginalNode id is "+workflowNode.getNodeId()+ " from workflow "+this.workflowId+" Executing nodeExecution ID " + workflowNodeExecution.getId() + " (Action: " + action
            //         + ", Provider: " + provider + ") with input: " + workflowNodeExecution.getInputData());
            NodeErrorLogger.logError("this is request data 78 "+requestData);
            outputData = executeNode(workflowNode, requestData);
            NodeErrorLogger.logError("outputData 79 is "+outputData);
            workflowNodeExecution.setOutputData(outputData);
            workflowNodeExecution = markNodeCompleted(workflowNodeExecution, outputData, workflowNode);
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
        return workflowNodeExecutionRepository.save(nodeExecution);
    }

    @Transactional
    protected WorkflowNodeExecution markNodeCompleted(WorkflowNodeExecution nodeExecution,
            Map<String, Object> outputData, WorkflowNodes workflowNode) {
        nodeExecution.setStatus(NodeStatus.COMPLETED);
        // Map<String, Object> existingOutputData = new HashMap<>();
        // existingOutputData.put("to", "khanyusuf0966@gmail.com");
        // existingOutputData.put("subject", "data from outputnode for node "+this.workFlowNodeId);
        // existingOutputData.put("message", "data from message");
        // existingOutputData.put("rawHtml",
        //         "<h2>Welcome to <b>RelayHooks</b>!</h2><p>We’re glad to have you. Start exploring your integrations today 🚀"+workflowNode.getNodeData()+
        //         " workflowNodeId-> "+this.workFlowNodeId+" workflowId-> "+this.workflowId+"</p>");
        // nodeExecution.setOutputData(existingOutputData);
        nodeExecution.setOutputData(outputData);
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
            NodeErrorLogger.logError("dependedt node is" + WorkflowNode.getNodeId());
            boolean allInputsDone = true;
            if (WorkflowNode.getInputNodes() != null && !WorkflowNode.getInputNodes().isEmpty()) {
                for (Long inputNodeId : WorkflowNode.getInputNodes()) {
                    WorkflowNodes inputNode = workflowNodeRepository.findByNodeIdAndWorkflowId(inputNodeId, this.workflowId)
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

            if (allInputsDone) {
                Boolean checkIfObjectEmpty = checkIfNodeDataObjectIsEmpty(WorkflowNode);
                if(checkIfObjectEmpty){
                    NodeErrorLogger.logError("Skipping nodeId " + WorkflowNode.getNodeId() + " as its 'object' field is empty."+", this is nodeData"+WorkflowNode.getNodeData());
                    continue;
                }
                Map<String, Object> payload = new HashMap<>();
                payload.put("OriginalworkflowNodeId", WorkflowNode.getNodeId());
                payload.put("workflowExecutionId", completedExecution.getWorkflowExecution().getId());
                payload.put("inputData", completedExecution.getOutputData());
                payload.put("workflowId", this.workflowId);
                rabbitTemplate.convertAndSend(RabbitMqConfig.EXECUTE_QUEUE, payload);
            } else {
                // NodeErrorLogger.logError("Not all inputs completed for nodeId " + WorkflowNode.getNodeId());
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
            if(objectMap == null || objectMap.isEmpty()){
                return true;
            }
            String apiUrlInObject = objectMap.getOrDefault("apiUrl", "").toString();
            return apiUrlInObject.isEmpty();
        }
        return false;
    }
}

/*
 * here inside never use database node id , always use combination of workflowId and workflowNodeId 
 */