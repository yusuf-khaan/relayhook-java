package com.app.relayhook.DTO;

import java.util.List;
import java.util.Map;

import com.app.relayhook.Enums.NodeType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowNodesDTO {

    private Long nodeId;

    private List<Long> inputNodes;

    private List<Long> outputNodes;

    private Map<String, Object> nodeData;

    private Integer retry = 3;

    private NodeType nodeType;

    private Boolean canExecuteParallel;

}
