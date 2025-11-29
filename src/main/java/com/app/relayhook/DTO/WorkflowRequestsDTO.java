package com.app.relayhook.DTO;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkflowRequestsDTO(
        Long id,
        String name,
        String email,
        String description,
        List<String> trigger,
        List<String> tags,
        String status,
        Integer progress,
        LocalDateTime scheduledTime
) {}
