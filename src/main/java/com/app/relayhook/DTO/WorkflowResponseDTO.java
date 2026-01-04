package com.app.relayhook.DTO;

import java.time.LocalDate;
import java.util.List;

public record WorkflowResponseDTO(
    Long id,
    String name,
    String description,
    List<String> trigger,
    Boolean canExecuteParallel,
    Boolean isActive,
    List<String> tags,
    String Webhookurl,
    LocalDate updatedAt
) {}
