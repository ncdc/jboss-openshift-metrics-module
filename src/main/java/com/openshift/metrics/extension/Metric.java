package com.openshift.metrics.extension;

public class Metric {
    private String sourceKey;
    private String publishKey;
    private boolean enabled;

    public Metric(String sourceKey, String publishKey, boolean enabled) {
        this.sourceKey = sourceKey;
        this.publishKey = publishKey;
        this.enabled = enabled;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((publishKey == null) ? 0 : publishKey.hashCode());
        result = prime * result
                + ((sourceKey == null) ? 0 : sourceKey.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        Metric other = (Metric) obj;
        if (publishKey == null) {
            if (other.publishKey != null) {
                return false;
            }
        } else if (!publishKey.equals(other.publishKey)) {
            return false;
        }
        if (sourceKey == null) {
            if (other.sourceKey != null) {
                return false;
            }
        } else if (!sourceKey.equals(other.sourceKey)) {
            return false;
        }
        return true;
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

    /**
     * @return the sourceKey
     */
    public String getSourceKey() {
        return sourceKey;
    }

    /**
     * @return the publishKey
     */
    public String getPublishKey() {
        return publishKey;
    }
}
