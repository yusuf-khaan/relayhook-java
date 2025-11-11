package com.app.relayhook.DTO;

import java.util.List;

public record WorkflowResponseDTO(
    Long id,
    String name,
    String description,
    List<String> trigger,
    Boolean canExecuteParallel,
    Boolean isActive,
    List<String> tags,
    Integer executionCount,
    String avatar,
    String url ,
    String batchClass
) {}
