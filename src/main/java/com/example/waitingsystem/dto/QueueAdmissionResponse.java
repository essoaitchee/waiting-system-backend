package com.example.waitingsystem.dto;

import java.util.List;

public record QueueAdmissionResponse(
	Long eventId,
	int requestedCount,
	int admittedCount,
	List<QueueAdmissionUserResponse> admittedUsers
) {
}
