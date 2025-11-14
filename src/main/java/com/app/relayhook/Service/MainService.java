package com.app.relayhook.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.app.relayhook.DTO.IntegrationsDTO;
import com.app.relayhook.DTO.WorkflowDTO;
import com.app.relayhook.DTO.WorkflowNodesDTO;
import com.app.relayhook.DTO.WorkflowRequestsDTO;
import com.app.relayhook.DTO.WorkflowResponseDTO;
import com.app.relayhook.Integrations.Relayhook.RelayhookAbs;
import com.app.relayhook.Logs.NodeErrorLogger;
import com.app.relayhook.Models.SystemIntegrations;
import com.app.relayhook.Models.Users;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Models.WorkflowNodes;
import com.app.relayhook.Models.WorkflowRequests;
import com.app.relayhook.Repository.SystemIntegrationsRepository;
import com.app.relayhook.Repository.UsersRepository;
import com.app.relayhook.Repository.WorkflowRepository;
import com.app.relayhook.Repository.WorkflowRequestsRepository;
import com.app.relayhook.SecurityConfig.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainService {

    private final WorkflowRepository workflowRepository;
    private final SystemIntegrationsRepository systemIntegrationsRepository;
    private final RelayhookAbs relayhookAbs;
    private final ObjectMapper objectMapper;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final WorkflowRequestsRepository workflowRequestsRepository;

    @Transactional
    public Workflow saveWorkflow(HttpServletRequest request, WorkflowDTO dto) {
        validateWorkflow(dto);
        Users users = usersRepository.findById((Long) request.getAttribute("userId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNAUTHORIZED"));
        Workflow workflow = new Workflow();
        workflow.setName(dto.getName());
        workflow.setUser(users);
        workflow.setDescription(dto.getDescription());
        workflow.setWorkflowData(objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {
        }));
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
                        node.setLevel(levelDTO.getLevel());
                        node.setNodeId(nodeDTO.getNodeId());
                        node.setInputNodes(nodeDTO.getInputSources());
                        node.setOutputNodes(nodeDTO.getOutputSources());
                        node.setNodeData(objectMapper.convertValue(nodeDTO.getNodeData(),
                                new TypeReference<Map<String, Object>>() {
                                }));
                        node.setNodeType(levelDTO.getNodeType());
                        node.setWorkflow(workflow);
                        node.setCanExecuteParallel(levelDTO.getCanExecuteParallel());
                        node.setRetriesLeft(nodeDTO.getRetry() != null ? nodeDTO.getRetry() : 3);
                        nodesList.add(node);
                    }
                }
            }
        }
        workflow.setWorkflowNodesData(nodesList);
        Workflow savedWorkflow = workflowRepository.save(workflow);
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

    public Object getAllIntegrations(Pageable pageable, String search) {
        Page<SystemIntegrations> providerPage;

        if (search == null || search.isBlank()) {
            providerPage = systemIntegrationsRepository.findAll(pageable);
        } else {
            providerPage = systemIntegrationsRepository
                    .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        }
        List<String> providerNames = providerPage.stream()
                .map(SystemIntegrations::getName)
                .collect(Collectors.toList());
        JsonNode providerListJson = objectMapper.valueToTree(providerPage);
        JsonNode providersMetaDataArray = relayhookAbs.getProvidersMetaData(providerNames);
        NodeErrorLogger.logError(providersMetaDataArray);
        Map<String, JsonNode> metadataMap = new HashMap<>();
        if (providersMetaDataArray != null && providersMetaDataArray.isArray()) {
            for (JsonNode node : providersMetaDataArray) {
                String providerKey = node.path("provider").asText();
                JsonNode metadata = node.path("metadata");
                if (!providerKey.isBlank() && !metadata.isMissingNode()) {
                    metadataMap.put(providerKey, metadata);
                }
            }
        }

        JsonNode contentNode = providerListJson.path("content");
        if (contentNode.isArray()) {
            for (JsonNode node : contentNode) {
                if (node.isObject()) {
                    ObjectNode objNode = (ObjectNode) node;
                    String providerKey = objNode.path("provider").asText();
                    JsonNode metadata = metadataMap.get(providerKey);
                    if (metadata != null) {
                        objNode.set("metadata", metadata);
                    }
                }
            }
        }

        return providerListJson;
    }

    public Users createUser(Users userDTO, HttpServletResponse response, HttpServletRequest request) {
        userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        String jwtToken = jwtUtil.generateToken(userDTO.getUsername());
        ResponseCookie cookie = ResponseCookie.from("jwt", jwtToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return usersRepository.save(userDTO);
    }

    public WorkflowRequests saveWorkflowRequest(Map<String, String> map, Long userId) {
        WorkflowRequests workflowRequests = new WorkflowRequests();
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNAUTHORIZED"));
        workflowRequests.setEmailToContact(map.getOrDefault("email", ""));
        workflowRequests.setDescription(map.getOrDefault("description", ""));
        workflowRequests.setName(map.getOrDefault("name", ""));
        Object value = map.get("scheduledTime");
        if (value instanceof String str && !str.isBlank()) {
            workflowRequests.setScheduledTimeToContact(LocalDateTime.parse(str));
        } else {
            workflowRequests.setScheduledTimeToContact(null);
        }
        workflowRequests.setUser(user);
        return workflowRequestsRepository.save(workflowRequests);
    }

    public Page<WorkflowRequestsDTO> getWorkflowRequest(HttpServletRequest request, String search,
            Pageable pageable) {
        Long userId = (Long) request.getAttribute("userId");
        Page<WorkflowRequests> workflowRequestsPage;
        if (search == null || search.isBlank()) {
            workflowRequestsPage = workflowRequestsRepository.findByUserId(userId, pageable);
        } else {
            workflowRequestsPage = workflowRequestsRepository.findByUserIdAndNameContainingIgnoreCase(userId, search,
                    pageable);
        }
        return workflowRequestsPage.map(req -> new WorkflowRequestsDTO(
                req.getId(),
                req.getName(),
                req.getDescription(),
                List.of("Manual"),
                // req.getTags(),
                // req.getTrigger(),
                List.of("User Request"),
                req.getStatus(),
                req.getProgress(),
                req.getUpdatedAt().toLocalDate()));
    }

    public Page<WorkflowResponseDTO> getUserWorkflows(HttpServletRequest request, String search, Pageable pageable) {
        Long userId = (Long) request.getAttribute("userId");
        Page<WorkflowResponseDTO> workflowPage;
        Page<Workflow> workflow;
        if (search == null || search.isBlank()) {
            workflow = workflowRepository.findByUserId(userId, pageable);
        } else {
            workflow = workflowRepository.findByUserIdAndNameContainingIgnoreCase(userId, search, pageable);
        }
        workflowPage = workflow.map(wf -> new WorkflowResponseDTO(
                wf.getId(),
                wf.getName(),
                wf.getDescription(),
                Stream.concat(wf.getTrigger().stream(), Stream.of("Webhook")).toList(),
                wf.getCanExecuteParallel(),
                wf.getIsActive(),
                List.of("Development"),
                wf.getExecutionCount(),
                wf.getWebhookUrl(),
                wf.getUpdatedAt().toLocalDate()));
        return workflowPage;
    }

    public Page<IntegrationsDTO> getIntegrations(Pageable pageable, String search) {
        Page<SystemIntegrations> integrationsPage;
        if (search == null || search.isBlank()) {
            integrationsPage = systemIntegrationsRepository.findAll(pageable);
        } else {
            integrationsPage = systemIntegrationsRepository
                    .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                            search, search, pageable);
        }
        return integrationsPage.map(integration -> new IntegrationsDTO(
                integration.getId(),
                integration.getName(),
                integration.getDescription(),
                integration.getCallbackUrl(),
                integration.getImage(),
                integration.getCategory(),
                integration.getAuthPayload()));
    }

}
