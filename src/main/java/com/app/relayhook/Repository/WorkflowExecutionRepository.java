package com.app.relayhook.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.Users;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Models.WorkflowExecution;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {

    Long countByWorkflow_Id(Long workflowId);
    WorkflowExecution findTopByWorkflowIdOrderByUpdatedAtDesc(Long workflowId);
    List<WorkflowExecution> findByWorkflow_IdOrderByCreatedAtDesc(Long workflowId);
    List<WorkflowExecution> findByWorkflow(Workflow workflow);
    Page<WorkflowExecution> findByWorkflow_IdAndWorkflow_User_Id(Long workflowId, Long userId, Pageable pageable);
}
