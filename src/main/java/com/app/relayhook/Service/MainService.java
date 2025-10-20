package com.app.relayhook.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.relayhook.DTO.WorkflowDTO;
import com.app.relayhook.DTO.WorkflowNodesDTO;
import com.app.relayhook.Models.SystemIntegrations;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Models.WorkflowNodes;
import com.app.relayhook.Repository.SystemIntegrationsRepository;
import com.app.relayhook.Repository.WorkflowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainService {

    private final WorkflowRepository workflowRepository;
    private final SystemIntegrationsRepository systemIntegrationsRepository;

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
        workflow.setMetadata(dto.getMetaData());

        List<WorkflowNodes> nodesList = new ArrayList<>();
        if (dto.getWorkflowNodesData() != null) {
            for (WorkflowNodesDTO levelDTO : dto.getWorkflowNodesData()) {
                if (levelDTO.getNodes() != null) {
                    for (WorkflowNodesDTO.LevelWrapper nodeDTO : levelDTO.getNodes()) {
                        WorkflowNodes node = new WorkflowNodes();
                        node.setNodeId(nodeDTO.getNodeId());
                        node.setInputNodes(nodeDTO.getInputSources());
                        node.setOutputNodes(nodeDTO.getOutputSources());
                        node.setNodeData(nodeDTO.getNodeData());
                        node.setNodeType(levelDTO.getNodeType());
                        node.setWorkflow(workflow);
                        node.setCanExecuteParallel(levelDTO.getCanExecuteParallel());
                        node.setRetriesLeft(nodeDTO.getRetry() != null ? nodeDTO.getRetry() : 3);

                        nodesList.add(node);
                    }
                }
            }
        }
        workflow.setNodes(nodesList);

        Workflow savedWorkflow = workflowRepository.save(workflow);
        log.info("Workflow saved with ID: " + savedWorkflow.getId());
        return savedWorkflow;
    }

    private void validateWorkflow(WorkflowDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow name is required.");
        }

        if (dto.getWorkflowNodesData() == null || dto.getWorkflowNodesData().isEmpty()) {
            throw new IllegalArgumentException("Workflow must contain at least one node level.");
        }

        List<WorkflowNodesDTO.LevelWrapper> allNodes = new ArrayList<>();
        for (WorkflowNodesDTO levelDTO : dto.getWorkflowNodesData()) {
            if (levelDTO.getNodes() != null) {
                allNodes.addAll(levelDTO.getNodes());
            }
        }

        if (allNodes.isEmpty()) {
            throw new IllegalArgumentException("Workflow must contain at least one node.");
        }

        List<Long> nodeIds = new ArrayList<>();
        for (WorkflowNodesDTO.LevelWrapper node : allNodes) {
            if (node.getNodeId() == null) {
                throw new IllegalArgumentException("Each node must have a nodeId.");
            }
            if (nodeIds.contains(node.getNodeId())) {
                throw new IllegalArgumentException("Duplicate nodeId found: " + node.getNodeId());
            }
            nodeIds.add(node.getNodeId());
        }

        for (WorkflowNodesDTO.LevelWrapper node : allNodes) {
            if (node.getInputSources() != null && node.getInputSources().contains(node.getNodeId())) {
                throw new IllegalArgumentException("Node " + node.getNodeId() + " cannot reference itself as input.");
            }
            if (node.getOutputSources() != null && node.getOutputSources().contains(node.getNodeId())) {
                throw new IllegalArgumentException("Node " + node.getNodeId() + " cannot reference itself as output.");
            }

            if (node.getInputSources() != null) {
                for (Long inputId : node.getInputSources()) {
                    if (!nodeIds.contains(inputId)) {
                        throw new IllegalArgumentException(
                                "Node " + node.getNodeId() + " has invalid input reference: " + inputId);
                    }
                }
            }

            if (node.getOutputSources() != null) {
                for (Long outputId : node.getOutputSources()) {
                    if (!nodeIds.contains(outputId)) {
                        throw new IllegalArgumentException(
                                "Node " + node.getNodeId() + " has invalid output reference: " + outputId);
                    }
                }
            }

            if (node.getNodeData() == null) {
                throw new IllegalArgumentException("Node " + node.getNodeId() + " must have nodeData defined.");
            }
        }
    }

    public Page<SystemIntegrations> getAllIntegrations(Pageable pageable, String search) {
        if (search == null || search.isBlank()) {
            return systemIntegrationsRepository.findAll(pageable);
        } else {
            return systemIntegrationsRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search, search, pageable);
        }
    }

}
