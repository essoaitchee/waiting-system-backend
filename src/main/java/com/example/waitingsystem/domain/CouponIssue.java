package com.example.waitingsystem.domain;

import java.time.LocalDateTime;

public class CouponIssue {

	private Long couponIssueId;
	private Long couponId;
	private String userId;
	private LocalDateTime issuedAt;

	public Long getCouponIssueId() {
		return couponIssueId;
	}

	public void setCouponIssueId(Long couponIssueId) {
		this.couponIssueId = couponIssueId;
	}

	public Long getCouponId() {
		return couponId;
	}

	public void setCouponId(Long couponId) {
		this.couponId = couponId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public LocalDateTime getIssuedAt() {
		return issuedAt;
	}

	public void setIssuedAt(LocalDateTime issuedAt) {
		this.issuedAt = issuedAt;
	}
}
