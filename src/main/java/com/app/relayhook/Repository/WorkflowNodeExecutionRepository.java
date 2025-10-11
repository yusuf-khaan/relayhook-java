package com.app.relayhook.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.WorkflowExecution;
import com.app.relayhook.Models.WorkflowNodeExecution;

@Repository
public interface WorkflowNodeExecutionRepository extends JpaRepository<WorkflowNodeExecution, Long> {

    List<WorkflowNodeExecution> findByWorkflowExecution(WorkflowExecution workflowExecution);

    Optional<WorkflowNodeExecution> findByWorkflowExecutionAndWorkflowNodeId(WorkflowExecution execution,
            Long inputNodeId);
}
