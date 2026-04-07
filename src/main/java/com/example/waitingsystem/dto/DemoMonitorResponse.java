package com.example.waitingsystem.dto;

import java.time.LocalDateTime;

public record DemoMonitorResponse(
	Long eventId,
	Long couponId,
	String couponName,
	String userId,
	Long queueLength,
	Long estimatedWaitSeconds,
	Integer admissionBatchSize,
	Long schedulerDelayMs,
	String congestionLevel,
	String queueStatus,
	String queueToken,
	Long queueNumber,
	Long currentPosition,
	Long aheadCount,
	String admissionToken,
	LocalDateTime admissionExpiresAt,
	Integer couponRemainingCount,
	Integer couponTotalCount,
	String couponStatus,
	int simulatedUsers,
	Long reactionTimeMs,
	Long clickedAtEpochMs
) {
}
