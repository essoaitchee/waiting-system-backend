package com.example.waitingsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.warm-up")
public class CacheWarmUpProperties {

	private boolean enabled = true;
	private int targetCount = 10000;
	private int pageSize = 100;
	private int maxPagesPerCategory = 100;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getTargetCount() {
		return targetCount;
	}

	public void setTargetCount(int targetCount) {
		this.targetCount = targetCount;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getMaxPagesPerCategory() {
		return maxPagesPerCategory;
	}

	public void setMaxPagesPerCategory(int maxPagesPerCategory) {
		this.maxPagesPerCategory = maxPagesPerCategory;
	}
}
