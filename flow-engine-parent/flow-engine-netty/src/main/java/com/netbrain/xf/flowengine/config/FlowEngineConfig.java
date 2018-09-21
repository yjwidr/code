package com.netbrain.xf.flowengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class FlowEngineConfig {
    @Value("${taskengine.ha.enabled}")
    private boolean hAEnabled;

    public boolean isHAEnabled() {
        return hAEnabled;
    }

    public void setHAEnabled(boolean haEnabled) {
        this.hAEnabled = haEnabled;
    }
}
