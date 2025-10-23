package com.app.relayhook.Integrations.Relayhook;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RelayhookClient implements RelayhookAbs {

    private RestTemplate restTemplate;
    private HttpHeaders headers = new HttpHeaders();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.relayhook.baseurl}")
    private String relayhookBaseUrl;

    @Autowired
    RelayhookClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        relayhookBaseUrl = relayhookBaseUrl + "/api";
    }

    private void getInstance() {
        headers.clear();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * Sends HTTP request and returns body as ObjectNode (mutable JSON).
     */
    public JsonNode sendRequest(
            String method,
            String url,
            Map<String, Object> params,
            boolean ignoreError) {
        url = relayhookBaseUrl+url;
        log.info(url);
        getInstance();
        method = method.toUpperCase();

        HttpEntity<Object> entity = "GET".equals(method)
                ? new HttpEntity<>(headers)
                : new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(method),
                    entity,
                    String.class);

            String jsonResponse = response.getBody();
            if (jsonResponse == null || jsonResponse.isBlank()) {
                return objectMapper.createObjectNode();
            }
            // Cast to ObjectNode for mutability
            return objectMapper.readTree(jsonResponse);

        } catch (Exception e) {
            if (!ignoreError) {
                throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
            }
            return objectMapper.createObjectNode();
        }
    }

    public String paramBuilder(String url, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        if (params != null && !params.isEmpty()) {
            params.forEach(builder::queryParam);
        }
        return builder.toUriString();
    }

    public JsonNode getProvidersMetaData(List<String> providersList) {
        return sendRequest("POST", "/api/integration/get-provider-metadata", Map.of("provider", providersList), false);
    }
}
