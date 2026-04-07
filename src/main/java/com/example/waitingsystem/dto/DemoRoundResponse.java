package com.example.waitingsystem.dto;

import java.util.List;

public record DemoRoundResponse(
	long serverNowEpochMs,
	long currentRoundStartEpochMs,
	long nextRoundStartEpochMs,
	long openWindowEndsAtEpochMs,
	long countdownMs,
	long phaseRemainingMs,
	int roundIntervalSeconds,
	int openWindowSeconds,
	boolean open,
	String phase,
	String currentRoundLabel,
	String leaderboardRoundLabel,
	Long myReactionTimeMs,
	Long myClickedAtEpochMs,
	List<DemoRoundLeaderboardEntryResponse> leaderboard
) {
}
