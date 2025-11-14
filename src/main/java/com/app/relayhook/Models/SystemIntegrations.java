package com.app.relayhook.Models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemIntegrations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Boolean active;

    private String Image;
    
     @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> category;

    private String description;

    private String authType;

    private String apiVersion;

    private String baseUrl;

    private String provider;

    private String callbackUrl;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String,String> authPayload;

    @OneToMany(mappedBy = "systemIntegrations")
    @JsonIgnore
    private List<UserIntegrationsCredentials> userIntegrationsCredentials;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
