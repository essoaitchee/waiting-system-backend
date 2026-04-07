package com.example.waitingsystem.dto;

public record DemoRoundLeaderboardEntryResponse(
	int rank,
	String userId,
	Long couponId,
	String couponName,
	long reactionTimeMs,
	long clickedAtEpochMs
) {
}
