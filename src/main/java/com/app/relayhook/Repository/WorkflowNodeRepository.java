package com.app.relayhook.Repository;

import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.WorkflowNodes;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface WorkflowNodeRepository extends JpaRepository<WorkflowNodes, Long> {

}
