package com.app.relayhook.DTO;

import java.util.List;
import java.time.LocalDate;

public record WorkflowRequestsDTO(
    Long id,
    String name,
    String description,
    List<String> trigger,
    List<String> tags,
    String status,
    Integer progress,
    LocalDate updatedAt
) {}
