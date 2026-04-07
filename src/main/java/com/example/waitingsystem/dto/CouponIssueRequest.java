package com.example.waitingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CouponIssueRequest(
	@NotNull(message = "couponId는 필수입니다.")
	Long couponId,
	@NotBlank(message = "userId는 필수입니다.")
	String userId
) {
}
