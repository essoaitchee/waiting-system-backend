package com.example.waitingsystem.dto;

public record AuthLoginResponse(
	String loginId,
	boolean created,
	String message
) {
}
