package com.app.relayhook.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.Users;
import com.app.relayhook.Models.Workflow;
import com.app.relayhook.Models.WorkflowRequests;

@Repository
public interface WorkflowRequestsRepository extends JpaRepository<WorkflowRequests, Long>{

    Page<WorkflowRequests> findByUserId(Long userId, Pageable pageable);

    Page<WorkflowRequests> findByUserIdAndNameContainingIgnoreCase(Long userId, String search, Pageable pageable);

}
