package com.example.waitingsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QueueAdmissionRequest(
	@NotNull(message = "eventId는 필수입니다.")
	Long eventId,
	@Min(value = 1, message = "count는 1 이상이어야 합니다.")
	Integer count
) {
}
