package com.example.waitingsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.queue")
public class QueueProperties {

	private int defaultAdmissionBatchSize = 100;
	private int defaultAdmissionWindowSeconds = 600;
	private Scheduler scheduler = new Scheduler();

	public int getDefaultAdmissionBatchSize() {
		return defaultAdmissionBatchSize;
	}

	public void setDefaultAdmissionBatchSize(int defaultAdmissionBatchSize) {
		this.defaultAdmissionBatchSize = defaultAdmissionBatchSize;
	}

	public int getDefaultAdmissionWindowSeconds() {
		return defaultAdmissionWindowSeconds;
	}

	public void setDefaultAdmissionWindowSeconds(int defaultAdmissionWindowSeconds) {
		this.defaultAdmissionWindowSeconds = defaultAdmissionWindowSeconds;
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public static class Scheduler {

		private boolean enabled = true;
		private long fixedDelayMs = 1000L;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public long getFixedDelayMs() {
			return fixedDelayMs;
		}

		public void setFixedDelayMs(long fixedDelayMs) {
			this.fixedDelayMs = fixedDelayMs;
		}
	}
}
