package com.example.waitingsystem.dto;

import jakarta.validation.constraints.NotBlank;

public record CouponResetRequest(
	@NotBlank(message = "userId를 입력해 주세요.")
	String userId
) {
}
