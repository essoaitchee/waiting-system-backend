package com.example.waitingsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
public class DemoProperties {

	private long defaultEventId = 1001L;
	private long defaultCouponId = 1L;
	private boolean trafficSimulationEnabled = true;
	private int trafficBurstSize = 120;
	private int admissionBatchSize = 12;
	private int admissionWindowSeconds = 180;
	private int minUsersBeforeCurrent = 60;
	private int maxUsersBeforeCurrent = 100;
	private int roundIntervalSeconds = 60;
	private int openWindowSeconds = 10;
	private String fakeUserPrefix = "crowd-user";

	public long getDefaultEventId() {
		return defaultEventId;
	}

	public void setDefaultEventId(long defaultEventId) {
		this.defaultEventId = defaultEventId;
	}

	public long getDefaultCouponId() {
		return defaultCouponId;
	}

	public void setDefaultCouponId(long defaultCouponId) {
		this.defaultCouponId = defaultCouponId;
	}

	public boolean isTrafficSimulationEnabled() {
		return trafficSimulationEnabled;
	}

	public void setTrafficSimulationEnabled(boolean trafficSimulationEnabled) {
		this.trafficSimulationEnabled = trafficSimulationEnabled;
	}

	public int getTrafficBurstSize() {
		return trafficBurstSize;
	}

	public void setTrafficBurstSize(int trafficBurstSize) {
		this.trafficBurstSize = trafficBurstSize;
	}

	public int getAdmissionBatchSize() {
		return admissionBatchSize;
	}

	public void setAdmissionBatchSize(int admissionBatchSize) {
		this.admissionBatchSize = admissionBatchSize;
	}

	public int getAdmissionWindowSeconds() {
		return admissionWindowSeconds;
	}

	public void setAdmissionWindowSeconds(int admissionWindowSeconds) {
		this.admissionWindowSeconds = admissionWindowSeconds;
	}

	public int getMinUsersBeforeCurrent() {
		return minUsersBeforeCurrent;
	}

	public void setMinUsersBeforeCurrent(int minUsersBeforeCurrent) {
		this.minUsersBeforeCurrent = minUsersBeforeCurrent;
	}

	public int getMaxUsersBeforeCurrent() {
		return maxUsersBeforeCurrent;
	}

	public void setMaxUsersBeforeCurrent(int maxUsersBeforeCurrent) {
		this.maxUsersBeforeCurrent = maxUsersBeforeCurrent;
	}

	public int getRoundIntervalSeconds() {
		return roundIntervalSeconds;
	}

	public void setRoundIntervalSeconds(int roundIntervalSeconds) {
		this.roundIntervalSeconds = roundIntervalSeconds;
	}

	public int getOpenWindowSeconds() {
		return openWindowSeconds;
	}

	public void setOpenWindowSeconds(int openWindowSeconds) {
		this.openWindowSeconds = openWindowSeconds;
	}

	public String getFakeUserPrefix() {
		return fakeUserPrefix;
	}

	public void setFakeUserPrefix(String fakeUserPrefix) {
		this.fakeUserPrefix = fakeUserPrefix;
	}
}
