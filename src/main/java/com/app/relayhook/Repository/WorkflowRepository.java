package com.app.relayhook.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.Users;
import com.app.relayhook.Models.Workflow;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long>{

    Page<Workflow> findByUserId(Long userId, Pageable pageable);

    Page<Workflow> findByUserIdAndNameContainingIgnoreCase(Long userId, String search, Pageable pageable);

    Long countByUserId(Long userId);

    Optional<Workflow> findByIdAndUserId(long workflowId, long userId);

    Workflow findByIdAndUser_Id(long workflowId, Long userId);
}
