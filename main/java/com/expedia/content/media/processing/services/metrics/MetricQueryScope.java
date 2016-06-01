package com.expedia.content.media.processing.services.metrics;

public enum MetricQueryScope {
  MONTHLY("-30d", "Monthly"),
  WEEKLY("-7d", "Weekly"),
  DAILY("-1d", "Daly");
    
    private final String value;
    private final String description;
    private MetricQueryScope(String value, String description) {
        this.value = value;
        this.description = description;
    }
    public String getValue() {
        return value;
    }
    public String getDescription() {
        return description;
    }       
}
