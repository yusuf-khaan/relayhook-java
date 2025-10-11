package com.app.relayhook.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.relayhook.DTO.WorkflowDTO;
import com.app.relayhook.DTO.WorkflowNodesDTO;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Models.WorkflowNodes;
import com.app.relayhook.Repository.WorkflowRepository;

import lombok.RequiredArgsConstructor;
import lombok.val;

@Service
@RequiredArgsConstructor
public class MainService {

    private final WorkflowRepository workflowRepository;

    @Transactional
    public Workflow saveWorkflow(WorkflowDTO dto) {
        validateWorkflow(dto);
        Workflow workflow = new Workflow();
        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        workflow.setWorkflowData(dto.getWorkflowData());
        workflow.setTrigger(dto.getTrigger());
        workflow.setSettings(dto.getSettings());
        workflow.setCanExecuteParallel(dto.getCanExecuteParallel());
        workflow.setIsActive(dto.getIsActive());
        workflow.setSchedule(dto.getSchedule());
        workflow.setWebhookUrl(dto.getWebhookUrl());
        workflow.setMetadata(dto.getMetadata());

        List<WorkflowNodes> nodesList = new ArrayList<>();
        if (dto.getNodes() != null) {
            for (WorkflowNodesDTO nodeDTO : dto.getNodes()) {
                WorkflowNodes node = new WorkflowNodes();
                node.setNodeId(nodeDTO.getNodeId());
                node.setInputNodes(nodeDTO.getInputNodes());
                node.setOutputNodes(nodeDTO.getOutputNodes());
                node.setNodeData(nodeDTO.getNodeData());
                node.setNodeType(nodeDTO.getNodeType());
                node.setWorkflow(workflow);
                node.setCanExecuteParallel(nodeDTO.getCanExecuteParallel());
                if (nodeDTO.getRetry() != null) {
                    node.setRetriesLeft(nodeDTO.getRetry());
                } else {
                    node.setRetriesLeft(3);
                }
                nodesList.add(node);
            }
        }
        workflow.setNodes(nodesList);
        Workflow savedWorkflow = workflowRepository.save(workflow);
        return savedWorkflow;
    }

    private void validateWorkflow(WorkflowDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow name is required.");
        }

        if (dto.getNodes() == null || dto.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Workflow must contain at least one node.");
        }

        // 2️⃣ Ensure unique node IDs
        List<Long> nodeIds = new ArrayList<>();
        for (WorkflowNodesDTO node : dto.getNodes()) {
            if (node.getNodeId() == null) {
                throw new IllegalArgumentException("Each node must have a nodeId.");
            }
            if (nodeIds.contains(node.getNodeId())) {
                throw new IllegalArgumentException("Duplicate nodeId found: " + node.getNodeId());
            }
            nodeIds.add(node.getNodeId());
        }

        // 3️⃣ Prevent self-loops
        for (WorkflowNodesDTO node : dto.getNodes()) {
            if (node.getInputNodes() != null && node.getInputNodes().contains(node.getNodeId())) {
                throw new IllegalArgumentException("Node " + node.getNodeId() + " cannot reference itself as input.");
            }
            if (node.getOutputNodes() != null && node.getOutputNodes().contains(node.getNodeId())) {
                throw new IllegalArgumentException("Node " + node.getNodeId() + " cannot reference itself as output.");
            }
        }

        // check if nodeIds which contain all nodes, and there is any input or output
        // node which is not in the nodeIds list
        for (WorkflowNodesDTO node : dto.getNodes()) {
            if (node.getInputNodes() != null) {
                for (Long inputId : node.getInputNodes()) {
                    if (!nodeIds.contains(inputId)) {
                        throw new IllegalArgumentException(
                                "Node " + node.getNodeId() + " has invalid input reference: " + inputId);
                    }
                }
            }
            if (node.getOutputNodes() != null) {
                for (Long outputId : node.getOutputNodes()) {
                    if (!nodeIds.contains(outputId)) {
                        throw new IllegalArgumentException(
                                "Node " + node.getNodeId() + " has invalid output reference: " + outputId);
                    }
                }
            }
        }

        for (WorkflowNodesDTO node : dto.getNodes()) {
            if (node.getNodeData() == null) {
                throw new IllegalArgumentException(
                        "Node " + node.getNodeId() + " must have nodeData defined.");
            }
        }

        // parallel execution check, maybe later allowing user to complete one level first then move to another or each node wait for atleast one input node to complete
        // and then execute
    }

}
