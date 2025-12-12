package com.app.relayhook.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetAllIntegrationDTO {

    private Long id;
    private String name;
    private Boolean active;
    private List<String> category;

    private AuthPayload authPayload;

    private String description;
    private String authType;
    private String apiVersion;
    private String baseUrl;
    private String provider;
    private String callbackUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String image;

    private Object metadata;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthPayload {
        private String scopes;
        private String client_id;
        private String client_secret;
    }
}
