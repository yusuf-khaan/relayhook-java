package com.app.relayhook.DTO;

import java.util.List;
import java.util.Map;

import org.slf4j.event.Level;

import com.app.relayhook.Enums.NodeType;
import com.app.relayhook.Models.WorkflowNodes.SchemaData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowNodesDTO {

    private Integer level;

    @Nullable
    private NodeType nodeType = null;

    private List<LevelWrapper> nodes;

    private Boolean canExecuteParallel;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LevelWrapper {
        private Integer nodeId;
        private Integer retry = 3;
        private List<Integer> inputSources;
        private List<Integer> outputSources;
        private NodeDataWrapper nodeData = null;
        private Map<Integer, List<SchemaData>> schemaData;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NodeDataWrapper {
        private Integer id;
        private double x;
        private double y;
        private Boolean isInitial =false;
        private IntegrationDataObjectWrapper object;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IntegrationDataObjectWrapper {
        private Long id;
        private String name;
        private Map<String,Object> defaultPayload;
        private String provider;
        private String description;
        private String action;
        private String apiUrl;
        private String image;
    }

    // @Data
    // @AllArgsConstructor
    // @NoArgsConstructor
    // public static class SchemaDataWrapper {
    //     private Map<Long, List<ProcessingSchemaWrapper>> schemaData;
    // }
}
