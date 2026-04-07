package com.example.waitingsystem.dto;

import java.time.LocalDateTime;

public record QueueStatusResponse(
	Long eventId,
	String userId,
	String status,
	String queueToken,
	Long queueNumber,
	Long currentPosition,
	Long aheadCount,
	LocalDateTime enteredAt,
	String admissionToken,
	LocalDateTime admittedAt,
	LocalDateTime admissionExpiresAt
) {
}
