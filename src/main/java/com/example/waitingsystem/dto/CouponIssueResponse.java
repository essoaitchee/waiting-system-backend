package com.example.waitingsystem.dto;

import java.time.LocalDateTime;

public record CouponIssueResponse(
	Long couponId,
	String userId,
	String status,
	Integer remainingCount,
	LocalDateTime issuedAt
) {
}
