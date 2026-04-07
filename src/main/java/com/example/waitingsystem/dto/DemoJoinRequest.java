package com.example.waitingsystem.dto;

public record DemoJoinRequest(
	Long eventId,
	Long couponId,
	String userId,
	Integer burstSize,
	Long clickedAtEpochMs
) {
}
