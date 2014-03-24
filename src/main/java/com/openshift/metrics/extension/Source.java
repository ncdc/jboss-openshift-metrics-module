package com.openshift.metrics.extension;

import java.util.HashMap;
import java.util.Map;

public class Source {
    private String path;
    private boolean mBean;
    private Map<String, String> metrics = new HashMap<String, String>();

    public Source(String path, boolean mBean) {
        this.path = path;
        this.mBean = mBean;
    }

    public void addMetric(String key, String publishName) {
        metrics.put(key, publishName);
    }

    public void removeMetric(String key) {
        metrics.remove(key);
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
     * @return the mBean
     */
    public boolean ismBean() {
        return mBean;
    }

    /**
     * @param mBean
     *            the mBean to set
     */
    public void setmBean(boolean mBean) {
        this.mBean = mBean;
    }

    /**
     * @return the metrics
     */
    public Map<String, String> getMetrics() {
        return metrics;
    }
}