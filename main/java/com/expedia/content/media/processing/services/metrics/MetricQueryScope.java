package com.expedia.content.media.processing.services.metrics;

public enum MetricQueryScope {
  MONTHLY("-30d"),
  WEEKLY("-7d"),
  DAILY("-1d");
    
    private final String value;
    private MetricQueryScope(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }    
}
