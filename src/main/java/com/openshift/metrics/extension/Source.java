package com.openshift.metrics.extension;

import java.util.HashMap;
import java.util.Map;

public class Source {
    private String path;
    private String type;
    private Map<String, Metric> metrics = new HashMap<String, Metric>();
    private boolean enabled;

    public Source(String path, String type, boolean enabled) {
        this.path = path;
        this.type = type;
        this.enabled = enabled;
    }

    public void addMetric(String sourceKey, String publishKey, boolean enabled) {
        metrics.put(publishKey, new Metric(sourceKey, publishKey, enabled));

    }

    public void enableMetric(String publishKey, boolean enabled) {
        final Metric metric = metrics.get(publishKey);
        if(metric != null) {
            metric.setEnabled(enabled);
        }
    }

    public void removeMetric(String publishKey) {
        metrics.remove(publishKey);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Source other = (Source) obj;
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        return true;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path
     *            the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the metrics
     */
    public Map<String, Metric> getMetrics() {
        return metrics;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}