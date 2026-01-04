package com.app.relayhook.Models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonType;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(
    value = { "id", "user", "createdAt", "updatedAt","workflowData", "workflowNodesData", "webhookUrl"},
    allowGetters = true
)
public class Workflow {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;
    private String avatar;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String,Object> workflowData;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> trigger;

     @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> settings;

    private Boolean canExecuteParallel = true;

    private Boolean isActive = true;

    private String schedule;

    private String webhookUrl;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = null;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Users user;

    @OneToMany(mappedBy = "workflow", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowNodes> workflowNodesData;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
