package com.app.relayhook.Models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import com.app.relayhook.Enums.NodeStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowNodeExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private WorkflowExecution workflowExecution;

    // this reference to the workflow postgres node id, not inner node id
    private Long workflowNodeId;

    @Enumerated(EnumType.STRING)
    private NodeStatus status = NodeStatus.PENDING;

    private Long retriesLeft = 3L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String,Object> inputData;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String,Object> outputData;

    private Long level;

    @ElementCollection
    private List<String> errorLogs;

    private Long executionTime;

    private Boolean canExecuteParallel;
}
