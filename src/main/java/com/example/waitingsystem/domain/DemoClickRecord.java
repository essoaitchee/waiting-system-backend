package com.example.waitingsystem.domain;

import java.time.LocalDateTime;

public class DemoClickRecord {

	private Long demoClickRecordId;
	private String userId;
	private Long couponId;
	private String couponName;
	private Long reactionTimeMs;
	private LocalDateTime clickedAt;
	private LocalDateTime roundStartedAt;
	private LocalDateTime createdAt;

	public Long getDemoClickRecordId() {
		return demoClickRecordId;
	}

	public void setDemoClickRecordId(Long demoClickRecordId) {
		this.demoClickRecordId = demoClickRecordId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Long getCouponId() {
		return couponId;
	}

	public void setCouponId(Long couponId) {
		this.couponId = couponId;
	}

	public String getCouponName() {
		return couponName;
	}

	public void setCouponName(String couponName) {
		this.couponName = couponName;
	}

	public Long getReactionTimeMs() {
		return reactionTimeMs;
	}

	public void setReactionTimeMs(Long reactionTimeMs) {
		this.reactionTimeMs = reactionTimeMs;
	}

	public LocalDateTime getClickedAt() {
		return clickedAt;
	}

	public void setClickedAt(LocalDateTime clickedAt) {
		this.clickedAt = clickedAt;
	}

	public LocalDateTime getRoundStartedAt() {
		return roundStartedAt;
	}

	public void setRoundStartedAt(LocalDateTime roundStartedAt) {
		this.roundStartedAt = roundStartedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
