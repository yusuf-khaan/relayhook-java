package com.app.relayhook.DTO;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WorkflowDTO {

    private Long id;

    private String name;

    private String description;

    private Map<String, Object> workflowData;

    private List<String> trigger;

    private Map<String, Object> settings;

    private Boolean canExecuteParallel = true;

    private Boolean isActive = true;

    private String schedule;

    private String webhookUrl;

    private Map<String, Object> metaData;

    private List<WorkflowNodesDTO> workflowNodesData;
}
