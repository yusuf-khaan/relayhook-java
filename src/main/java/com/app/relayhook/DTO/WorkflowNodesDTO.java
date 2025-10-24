package com.app.relayhook.DTO;

import java.util.List;
import java.util.Map;

import org.slf4j.event.Level;

import com.app.relayhook.Enums.NodeType;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowNodesDTO {

    private Long level;

    @Nullable
    private NodeType nodeType = null;

    private List<LevelWrapper> nodes;

    private Boolean canExecuteParallel;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LevelWrapper {
        private Long nodeId;
        private Integer retry = 3;
        private List<Long> inputSources;
        private List<Long> outputSources;
        private NodeDataWrapper nodeData;
        private SchemaDataWrapper schemaData;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NodeDataWrapper {
        private Long id;
        private Long x;
        private Long y;
        private IntegrationDataObjectWrapper object;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IntegrationDataObjectWrapper {
        private Long id;
        private String name;
        private String description;
        private String action;
        private String apiUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SchemaDataWrapper {
        private Long nodeId;
        private List<ProcessingSchemaWrapper> processingSchema;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessingSchemaWrapper {
        private String inputKey;
        private String mappedWith;
    }
}
