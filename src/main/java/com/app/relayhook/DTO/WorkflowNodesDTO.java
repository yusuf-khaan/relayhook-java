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
        private Map<String, Object> nodeData;
        private Map<String, Object> inputSchema;
        private Map<String, Object> processingSchema;
        private Map<String, Object> outputSchema;
    }

}
