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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RelayhookClient implements RelayhookAbs {

    private RestTemplate restTemplate;
    private HttpHeaders headers = new HttpHeaders();


    @Value("${spring.relayhook.baseurl}")
    private String relayhookBaseUrl;

    @Autowired
    RelayhookClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        relayhookBaseUrl = relayhookBaseUrl+"/api";
    }

    private void getInstance() {
        headers.clear();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    /**
     * Sends HTTP request and returns only the body as String.
     */
    public String sendRequest(String method, String url, Map<String, Object> params, boolean ignoreError) {
        url = relayhookBaseUrl+url;
        getInstance();
        method = method.toUpperCase();
        HttpEntity<Object> entity = new HttpEntity<>(null);
        if (!method.toLowerCase().equals("get")) {
            entity = new HttpEntity<>(params, headers);
        } else {
            url = paramBuilder(url, params);
        }

        try {
            ResponseEntity<Map<String,Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(method),
                    entity,
                    String.class);

            return response.getBody(); // ✅ Only JSON body

        } catch (HttpServerErrorException e) {
            if (!ignoreError) {
                throw new RuntimeException("Server error: " + e.getResponseBodyAsString(), e);
            }
            return e.getResponseBodyAsString();
        }
    }

    public String paramBuilder(String url, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        if (params != null && !params.isEmpty()) {
            params.forEach(builder::queryParam);
        }
        return builder.toUriString();
    }

    public Map<String,Object> getProvidersMetaData(List<String> providersList){
        return sendRequest("POST", "/integration/get-provider-metadata", Map.of("provider",providersList), false);
    }

}