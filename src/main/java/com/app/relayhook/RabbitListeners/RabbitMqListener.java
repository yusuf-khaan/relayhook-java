package com.app.relayhook.RabbitListeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.relayhook.Configs.RabbitMqConfig;
import com.app.relayhook.Enums.NodeStatus;
import com.app.relayhook.Models.WorkflowExecution;
import com.app.relayhook.Models.WorkflowNodeExecution;
import com.app.relayhook.Models.WorkflowNodes;
import com.app.relayhook.Repository.WorkflowNodeExecutionRepository;
import com.app.relayhook.Repository.WorkflowNodeRepository;
import com.app.relayhook.Repository.WorkflowRepository;
import com.app.relayhook.Service.WorkflowExecutorService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RabbitMqListener {

    private final WorkflowNodeExecutionRepository nodeExecutionRepo;
    private final WorkflowExecutorService workflowExecutorService;
    private final WorkflowNodeRepository nodeRepo;
    private final WorkflowRepository workflowRepository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    @Transactional
    public void processNode(Long nodeExecutionId) {
        WorkflowNodeExecution nodeExecution = nodeExecutionRepo.findById(nodeExecutionId).orElseThrow();
        WorkflowNodes node = nodeRepo.findById(nodeExecution.getWorkflowNodeId()).orElseThrow();

        try {
            // Execute node (call 3rd-party API or internal logic)
            Map<String, Object> outputData = executeNode(node, nodeExecution.getInputData());

            // Update node execution
            nodeExecution.setStatus(NodeStatus.COMPLETED);
            nodeExecution.setOutputData(outputData);
            nodeExecutionRepo.save(nodeExecution);

            // Schedule next nodes
            scheduleNextNodes(nodeExecution);

        } catch (Exception e) {
            nodeExecution.setStatus(NodeStatus.FAILED);
            nodeExecution.getErrorLogs().add(e.getMessage());
            nodeExecutionRepo.save(nodeExecution);
        }
    }

    private Map<String, Object> executeNode(WorkflowNodes node, Map<String, Object> inputData) {
        // Implement your node logic here
        return Map.of("result", "success");
    }

    private void scheduleNextNodes(WorkflowNodeExecution completedNode) {
        WorkflowExecution execution = completedNode.getWorkflowExecution();

        // Find all nodes of the workflow
        List<WorkflowNodeExecution> allNodes = nodeExecutionRepo.findByWorkflowExecution(execution);

        // Group nodes by level
        Map<Integer, List<WorkflowNodeExecution>> levelMap = new HashMap<>();
        for (WorkflowNodeExecution ne : allNodes) {
            WorkflowNodes node = nodeRepo.findById(ne.getWorkflowNodeId()).orElseThrow();
            int level = node.getLevel().intValue();
            levelMap.computeIfAbsent(level, k -> new ArrayList<>()).add(ne);
        }

        // Find current completed node's level
        WorkflowNodes completedNodeMeta = nodeRepo.findById(completedNode.getWorkflowNodeId()).orElseThrow();
        Long currentLevel = completedNodeMeta.getLevel();

        // Find next level
        Long nextLevel = currentLevel + 1L;
        List<WorkflowNodeExecution> nextLevelNodes = levelMap.get(nextLevel);
        if (nextLevelNodes == null || nextLevelNodes.isEmpty())
            return;

        // Check parallelism for this level
        boolean canParallel = nextLevelNodes.get(0).getCanExecuteParallel(); // all nodes in level
                                                                             // share this

        for (WorkflowNodeExecution nextNodeExecution : nextLevelNodes) {
            WorkflowNodes nextNodeMeta = nodeRepo.findById(nextNodeExecution.getWorkflowNodeId()).orElseThrow();

            // Check if all input nodes are completed
            boolean ready = true;
            for (Long inputNodeId : nextNodeMeta.getInputNodes()) {
                WorkflowNodeExecution inputExecution = nodeExecutionRepo
                        .findByWorkflowExecutionAndWorkflowNodeId(execution, inputNodeId)
                        .orElseThrow();
                if (inputExecution.getStatus() != NodeStatus.COMPLETED) {
                    ready = false;
                    break;
                }
            }

            // Enqueue if ready
            if (ready && nextNodeExecution.getStatus() == NodeStatus.PENDING) {
                if (!canParallel) {
                    rabbitTemplate.convertAndSend(
                            RabbitMqConfig.EXCHANGE_NAME,
                            RabbitMqConfig.ROUTING_KEY,
                            nextNodeExecution.getId());
                    break; // stop after scheduling one if serial
                } else {
                    rabbitTemplate.convertAndSend(
                            RabbitMqConfig.EXCHANGE_NAME,
                            RabbitMqConfig.ROUTING_KEY,
                            nextNodeExecution.getId());
                }
            }
        }

    }

}