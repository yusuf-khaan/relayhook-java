package com.app.relayhook.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.Users;
import com.app.relayhook.Models.WorkflowExecution;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {

    Long countByWorkflow_Id(Long workflowId);
    WorkflowExecution findTopByWorkflowIdOrderByUpdatedAtDesc(Long workflowId);
    List<WorkflowExecution> findByWorkflow_IdOrderByCreatedAtDesc(Long workflowId);
}
