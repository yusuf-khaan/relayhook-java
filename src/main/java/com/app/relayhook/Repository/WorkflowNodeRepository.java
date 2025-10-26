package com.app.relayhook.Repository;

import org.springframework.stereotype.Repository;

import com.app.relayhook.Models.WorkflowNodes;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface WorkflowNodeRepository extends JpaRepository<WorkflowNodes, Long> {

     @EntityGraph(attributePaths = {"inputNodes", "outputNodes"})
    Optional<WorkflowNodes> findWithNodesById(Long id);

}
