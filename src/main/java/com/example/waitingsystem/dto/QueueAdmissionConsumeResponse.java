package com.example.waitingsystem.dto;

import java.time.LocalDateTime;

public record QueueAdmissionConsumeResponse(
	Long eventId,
	String userId,
	String status,
	LocalDateTime processedAt
) {
}
