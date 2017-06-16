package com.expedia.content.media.processing.services.init;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.expedia.www.platform.monitoring.configuration.MonitoringAgentConfiguration;

public class MonitoringAgentConfig {

    //Suppressing PMD warning for ImmutableField as this is injected by Spring's
    //ConfigureProperties annotation in InitSupport
    @SuppressWarnings("PMD.ImmutableField")
    private Map<String, String> monitoringAgent = new HashMap<>();

    public void setHostName(String hostName, String version) {
        monitoringAgent.put(MonitoringAgentConfiguration.APPLICATION_HOSTNAME,
                            getShortApplicationVersionPrefix(version) + hostName);
    }

    public Map<String, String> getMonitoringAgent() {
        return monitoringAgent;
    }

    private String getShortApplicationVersionPrefix(String applicationVersion) {
        if (StringUtils.hasText(applicationVersion)) {
            if (applicationVersion.length() > 6) {
                return applicationVersion.substring(0, 6) + '-';
            }
            return applicationVersion + '-';
        }
        return "";
    }
}
