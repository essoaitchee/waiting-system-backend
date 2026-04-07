package com.example.waitingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QueueAdmissionConsumeRequest(
	@NotNull(message = "eventId는 필수입니다.")
	Long eventId,
	@NotBlank(message = "admissionToken은 필수입니다.")
	String admissionToken
) {
}
