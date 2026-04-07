package com.example.waitingsystem.dto;

import java.time.LocalDateTime;

public record QueueAdmissionUserResponse(
	String userId,
	String queueToken,
	String admissionToken,
	LocalDateTime admissionExpiresAt
) {
}
