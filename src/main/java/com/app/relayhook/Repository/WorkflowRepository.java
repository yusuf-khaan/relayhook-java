package com.app.relayhook.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.Workflow;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long>{

}
