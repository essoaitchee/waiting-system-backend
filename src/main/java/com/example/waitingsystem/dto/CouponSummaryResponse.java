package com.example.waitingsystem.dto;

import java.time.LocalDateTime;

public record CouponSummaryResponse(
	Long couponId,
	String couponName,
	Integer totalCount,
	Integer remainCount,
	String status,
	boolean available,
	boolean issued,
	LocalDateTime issuedAt
) {
}
