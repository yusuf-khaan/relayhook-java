package com.app.relayhook.DTO;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WorkflowDTO {

    private String name;

    private String description;

    private Map<String, Object> workflowData;

    private Map<String, Object> trigger;

    private Map<String, Object> settings;

    private Boolean canExecuteParallel = true;

    private Boolean isActive = true;

    private String schedule;

    private String webhookUrl;

    private Map<String, Object> metaData;

    private List<WorkflowNodesDTO> workflowNodesData;
}
