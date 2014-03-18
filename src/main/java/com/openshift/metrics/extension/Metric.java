package com.openshift.metrics.extension;


public class Metric {
	private String key;
	private String publishName;
	
	public Metric() {
	}
	
	public Metric(String key, String publishName) {
		this.key = key;
		this.publishName = publishName;
	}
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public String getPublishName() {
		return publishName;
	}
	
	public void setPublishName(String publishName) {
		this.publishName = publishName;
	}
}
