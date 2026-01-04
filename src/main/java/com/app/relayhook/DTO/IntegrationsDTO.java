package com.app.relayhook.DTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record IntegrationsDTO(
    Long id,
    String name,
    String description,
    String OauthUrl,
    String image,
    List<String> category,
    Map<String, Object> integrationDetailObject,
    String slug
) {}
