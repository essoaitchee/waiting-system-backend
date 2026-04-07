package com.example.waitingsystem.dto;

public record CouponResetResponse(
	String userId,
	int resetCount,
	String message
) {
}
