package com.app.relayhook.Models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import com.app.relayhook.Enums.NodeStatus;
import com.app.relayhook.Enums.NodeType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.annotation.Generated;
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
public class WorkflowNodes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer level;
    private Integer nodeId;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Integer> inputNodes;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Integer> outputNodes;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<Integer, List<SchemaData>> schemaData;

    private String action = null;
    private String provider = null;


    private Boolean canExecuteParallel = true;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> nodeData;

    @Enumerated(EnumType.STRING)
    private NodeStatus status = NodeStatus.PENDING;

    private int retriesLeft = 3;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> output;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnore
    private Workflow workflow;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private NodeType nodeType = NodeType.INTEGRATION;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SchemaData {
        private String inputKey;
        private String mappedWith;
    }
}
