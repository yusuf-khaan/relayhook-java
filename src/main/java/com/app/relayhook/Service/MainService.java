package com.app.relayhook.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import com.app.relayhook.Configs.AppConfig;
import com.app.relayhook.DTO.IntegrationsDTO;
import com.app.relayhook.DTO.WorkflowDTO;
import com.app.relayhook.DTO.WorkflowNodesDTO;
import com.app.relayhook.DTO.WorkflowRequestsDTO;
import com.app.relayhook.DTO.WorkflowResponseDTO;
import com.app.relayhook.Integrations.Relayhook.RelayhookAbs;
import com.app.relayhook.Logs.NodeErrorLogger;
import com.app.relayhook.Models.ScheduleChanges;
import com.app.relayhook.Models.SystemIntegrations;
import com.app.relayhook.Models.UserIntegrationsCredentials;
import com.app.relayhook.Models.Users;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Models.WorkflowExecution;
import com.app.relayhook.Models.WorkflowNodes;
import com.app.relayhook.Models.WorkflowRequests;
import com.app.relayhook.Repository.ScheduledChangesRepository;
import com.app.relayhook.Repository.SystemIntegrationsRepository;
import com.app.relayhook.Repository.UserIntegrationsCredentialsRepository;
import com.app.relayhook.Repository.UsersRepository;
import com.app.relayhook.Repository.WorkflowExecutionRepository;
import com.app.relayhook.Repository.WorkflowNodeExecutionRepository;
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
    private final ScheduledChangesRepository scheduledChangesRepository;
    private final UserIntegrationsCredentialsRepository userIntegrationsCredentialsRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WorkflowNodeExecutionRepository workflowNodeExecutionRepository;

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
                        node.setSchemaData(nodeDTO.getSchemaData());
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

        List<Integer> nodeIds = new ArrayList<>();
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
                for (Integer inputId : node.getInputSources()) {
                    if (!nodeIds.contains(inputId)) {
                        throw new IllegalArgumentException(
                                "Node " + node.getNodeId() + " has invalid input reference: " + inputId);
                    }
                }
            }

            if (node.getOutputSources() != null) {
                for (Integer outputId : node.getOutputSources()) {
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
        if (search.isEmpty()) {
            providerPage = systemIntegrationsRepository.findAll(pageable);
        } else {
            providerPage = systemIntegrationsRepository
                    .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        }
        List<String> providerList = new ArrayList<>();
        for (SystemIntegrations system : providerPage.getContent()) {
            providerList.add(system.getProvider());
        }
        Object providersMetaData = relayhookAbs.getProvidersMetaData(providerList);
        return providersMetaData;
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

    public WorkflowRequests saveWorkflowRequest(WorkflowRequestsDTO workflowRequestsDTO, Long userId) {
        WorkflowRequests workflowRequests = new WorkflowRequests();
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNAUTHORIZED"));
        workflowRequests.setEmailToContact(workflowRequestsDTO.email());
        workflowRequests.setDescription(workflowRequestsDTO.description());
        workflowRequests.setName(workflowRequestsDTO.name());
        workflowRequests.setTags(workflowRequestsDTO.tags());
        workflowRequests.setTrigger(workflowRequestsDTO.trigger());
        workflowRequests.setScheduledTimeToContact(workflowRequestsDTO.scheduledTime());
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
                req.getEmailToContact(),
                req.getDescription(),
                req.getTrigger(),
                req.getTags(),
                req.getStatus(),
                req.getProgress(),
                req.getScheduledTimeToContact()));
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
        Page<SystemIntegrations> systemIntegrations;
        if (search == null || search.isBlank()) {
            systemIntegrations = systemIntegrationsRepository.findAll(pageable);
        } else {
            systemIntegrations = systemIntegrationsRepository
                    .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                            search, search, pageable);
        }
        return systemIntegrations.map(integration -> new IntegrationsDTO(
                integration.getId(),
                integration.getName(),
                integration.getDescription(),
                integration.getCallbackUrl(),
                integration.getImage(),
                integration.getCategory(),
                integration.getAuthPayload(),
                integration.getProvider()));
    }

    public Page<ScheduleChanges> getScheduledChanges(Pageable pageable, String search, Long userId,
            Long workflowRequestId) {
        Page<ScheduleChanges> scheduleChanges;
        if (search == null || search.isBlank()) {
            scheduleChanges = scheduledChangesRepository.findAllByWorkflowRequestId(workflowRequestId, pageable);
        } else {
            scheduleChanges = scheduledChangesRepository
                    .search(
                            userId, search, pageable);
        }
        return scheduleChanges;
    }

    public ScheduleChanges saveScheduleChanges(Map<String, Object> scheduleMap, Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNAUTHORIZED"));
        ScheduleChanges sc = objectMapper.convertValue(scheduleMap, ScheduleChanges.class);
        sc.setUser(user);
        return scheduledChangesRepository.save(sc);
    }

    public UserIntegrationsCredentials saveUserIntegration(Map<String, Object> integrationDetail, Long userId) {
        Long integrationId = ((Number) integrationDetail.get("integrationId")).longValue();
        String slug = integrationDetail.get("slug").toString();
        Optional<UserIntegrationsCredentials> existingOpt = userIntegrationsCredentialsRepository
                .findBySlugAndUser_Id(slug, userId);
        UserIntegrationsCredentials credentials;
        if (existingOpt.isPresent()) {
            credentials = existingOpt.get();
            Map<String, String> authDetail = objectMapper.convertValue(
                    integrationDetail.get("authDetail"),
                    new TypeReference<Map<String, String>>() {
                    });
            credentials.setAuthDetail(authDetail);
        } else {
            credentials = objectMapper.convertValue(
                    integrationDetail,
                    UserIntegrationsCredentials.class);
            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            credentials.setUser(user);
            Map<String, String> authDetail = objectMapper.convertValue(
                    integrationDetail.get("authDetail"),
                    new TypeReference<Map<String, String>>() {
                    });
            credentials.setSlug(slug);
            credentials.setAuthDetail(authDetail);
        }
        return userIntegrationsCredentialsRepository.save(credentials);
    }

    public Map<String, String> updateUserDetails(Map<String, String> map, Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String password = map.get("password");
        String email = map.get("email");
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        if (email != null && !email.isEmpty()) {
            user.setEmail(email);
        }
        usersRepository.save(user);
        return Map.of("message", "User details updated successfully");
    }

    public Map<String, String> sendNewPassword(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        String newPassword = generateRandomPassword(10);
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);
        Map<String, String> response = new HashMap<>();
        response.put("newPassword", newPassword);
        response.put("message", "New password generated successfully!");
        return response;
    }

    private String generateRandomPassword(int length) {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%!&*";
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    public Map<String, Object> me(Long userId) {
        Users users = usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not Authorized"));
        Long totalWorkflows = workflowRepository.countByUserId(userId);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", users.getUsername());
        userMap.put("email", users.getEmail());
        userMap.put("avatar", users.getAvatar());
        userMap.put("totalWorkflows", totalWorkflows);
        return userMap;
    }

    public Map<String, Object> getWorkflowStatistics(Long userId, Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow not found"));
        Long numberOfTimesWorkflowExecuted = workflowExecutionRepository.countByWorkflow_Id(workflowId);
        WorkflowExecution workflowExecution = workflowExecutionRepository.findTopByWorkflowIdOrderByUpdatedAtDesc(workflowId);
        Long numberOfNodes = (long) workflow.getWorkflowNodesData().size();
        Map<String, Object> workflowDetail = new HashMap<>();
        workflowDetail.put("workflow", workflow);
        workflowDetail.put("name", workflow.getName());
        workflowDetail.put("executionCount", numberOfTimesWorkflowExecuted);
        workflowDetail.put("numberOfNodes", numberOfNodes);
        workflowDetail.put("lastExecutedAt", workflowExecution != null ? workflowExecution.getUpdatedAt() : null);
        workflowDetail.put("triggers", workflow.getTrigger());
        workflowDetail.put("tags", workflow.getTags());
        workflowDetail.put("scheduledAt", workflow.getSchedule());
        return workflowDetail;
    }

    public Workflow updateWorkflow(Long workflowId, Map<String, Object> updates){
         Workflow existing = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow not found"));
        try{
        objectMapper.updateValue(existing, updates);
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid update data");
        }
        return workflowRepository.save(existing);
    }

     public List<WorkflowExecution> getWorkflowExecution(Long userId, Long workflowId){
         List<WorkflowExecution> existing = workflowExecutionRepository.findByWorkflow_IdOrderByCreatedAtDesc(workflowId);
        return existing;
    }
}
