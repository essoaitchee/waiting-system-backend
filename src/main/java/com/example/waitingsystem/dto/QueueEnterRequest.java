package com.example.waitingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QueueEnterRequest(
	@NotNull(message = "eventId는 필수입니다.")
	Long eventId,
	@NotBlank(message = "userId는 필수입니다.")
	String userId
) {
}
