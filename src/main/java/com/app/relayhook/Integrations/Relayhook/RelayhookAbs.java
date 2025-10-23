package com.app.relayhook.Integrations.Relayhook;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface RelayhookAbs {

    abstract public JsonNode getProvidersMetaData(List<String> providersList);

}
