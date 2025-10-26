package com.app.relayhook.Integrations.Relayhook;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface RelayhookAbs {

    abstract public JsonNode getProvidersMetaData(List<String> providersList);
    abstract public JsonNode executeAutomationRequest(Map<String,Object> map, String action, String provider);

}
