package com.app.relayhook.Integrations.Relayhook;

import java.util.List;
import java.util.Map;

public interface RelayhookAbs {

    abstract public Map<String,Object> getProvidersMetaData(List<String> providersList);

}
