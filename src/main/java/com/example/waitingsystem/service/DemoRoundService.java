package com.example.waitingsystem.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.waitingsystem.common.exception.ConflictException;
import com.example.waitingsystem.config.DemoProperties;
import com.example.waitingsystem.domain.DemoClickRecord;
import com.example.waitingsystem.dto.DemoRoundLeaderboardEntryResponse;
import com.example.waitingsystem.dto.DemoRoundResponse;
import com.example.waitingsystem.repository.DemoRoundRepository;
import org.springframework.stereotype.Service;

@Service
public class DemoRoundService {

	private static final DateTimeFormatter ROUND_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
		.withZone(ZoneId.systemDefault());

	private final DemoProperties demoProperties;
	private final DemoRoundRepository demoRoundRepository;
	private final Clock clock;

	public DemoRoundService(DemoProperties demoProperties, DemoRoundRepository demoRoundRepository, Clock clock) {
		this.demoProperties = demoProperties;
		this.demoRoundRepository = demoRoundRepository;
		this.clock = clock;
	}

	public DemoRoundResponse getRound(String userId) {
		RoundWindow roundWindow = resolveWindow();
		DemoClickRecord myClickRecord = demoRoundRepository.findBestClickRecordByUserId(userId);

		return new DemoRoundResponse(
			roundWindow.nowEpochMs(),
			roundWindow.currentRoundStartEpochMs(),
			roundWindow.nextRoundStartEpochMs(),
			roundWindow.openWindowEndsAtEpochMs(),
			roundWindow.countdownMs(),
			roundWindow.phaseRemainingMs(),
			demoProperties.getRoundIntervalSeconds(),
			demoProperties.getOpenWindowSeconds(),
			roundWindow.isOpen(),
			roundWindow.isOpen() ? "OPEN" : "COUNTDOWN",
			toRoundLabel(roundWindow.currentRoundStartEpochMs()),
			"ALL RECORDS",
			myClickRecord == null ? null : myClickRecord.getReactionTimeMs(),
			myClickRecord == null ? null : toEpochMs(myClickRecord.getClickedAt()),
			toLeaderboard(demoRoundRepository.findTopClickRecords(10)));
	}

	public ClickRecord recordClick(String userId, Long couponId, String couponName) {
		RoundWindow roundWindow = resolveWindow();
		if (!roundWindow.isOpen()) {
			throw new ConflictException("아직 쿠폰 오픈 전입니다. 카운트다운이 끝난 뒤 다시 시도해 주세요.");
		}

		LocalDateTime roundStartedAt = toLocalDateTime(roundWindow.currentRoundStartEpochMs());
		DemoClickRecord existingRecord = demoRoundRepository.findRoundClickByUserId(userId, roundStartedAt);
		if (existingRecord != null) {
			return toClickRecord(existingRecord);
		}

		long clickedAtEpochMs = roundWindow.nowEpochMs();
		DemoClickRecord clickRecord = new DemoClickRecord();
		clickRecord.setUserId(userId);
		clickRecord.setCouponId(couponId);
		clickRecord.setCouponName(couponName);
		clickRecord.setReactionTimeMs(Math.max(clickedAtEpochMs - roundWindow.currentRoundStartEpochMs(), 0L));
		clickRecord.setClickedAt(toLocalDateTime(clickedAtEpochMs));
		clickRecord.setRoundStartedAt(roundStartedAt);
		clickRecord.setCreatedAt(LocalDateTime.now(clock));
		demoRoundRepository.insertClickRecord(clickRecord);
		return toClickRecord(clickRecord);
	}

	public ClickRecord findLatestClickRecord(String userId) {
		DemoClickRecord latestRecord = demoRoundRepository.findLatestClickRecordByUserId(userId);
		return latestRecord == null ? null : toClickRecord(latestRecord);
	}

	public ClickRecord findBestClickRecord(String userId) {
		DemoClickRecord bestRecord = demoRoundRepository.findBestClickRecordByUserId(userId);
		return bestRecord == null ? null : toClickRecord(bestRecord);
	}

	private List<DemoRoundLeaderboardEntryResponse> toLeaderboard(List<DemoClickRecord> clickRecords) {
		return java.util.stream.IntStream.range(0, clickRecords.size())
			.mapToObj(index -> {
				DemoClickRecord record = clickRecords.get(index);
				return new DemoRoundLeaderboardEntryResponse(
					index + 1,
					record.getUserId(),
					record.getCouponId(),
					record.getCouponName(),
					record.getReactionTimeMs(),
					toEpochMs(record.getClickedAt()));
			})
			.toList();
	}

	private ClickRecord toClickRecord(DemoClickRecord clickRecord) {
		return new ClickRecord(
			clickRecord.getUserId(),
			clickRecord.getCouponId(),
			clickRecord.getCouponName(),
			clickRecord.getReactionTimeMs(),
			toEpochMs(clickRecord.getClickedAt()));
	}

	private RoundWindow resolveWindow() {
		long nowEpochMs = Instant.now(clock).toEpochMilli();
		long roundIntervalMs = Math.max(demoProperties.getRoundIntervalSeconds(), 1) * 1000L;
		long openWindowMs = Math.min(Math.max(demoProperties.getOpenWindowSeconds(), 1) * 1000L, roundIntervalMs);
		long currentRoundStartEpochMs = (nowEpochMs / roundIntervalMs) * roundIntervalMs;
		long elapsedMs = nowEpochMs - currentRoundStartEpochMs;
		boolean open = elapsedMs < openWindowMs;
		long nextRoundStartEpochMs = currentRoundStartEpochMs + roundIntervalMs;
		long previousRoundStartEpochMs = Math.max(0L, currentRoundStartEpochMs - roundIntervalMs);
		long openWindowEndsAtEpochMs = currentRoundStartEpochMs + openWindowMs;
		long countdownMs = open ? 0L : Math.max(nextRoundStartEpochMs - nowEpochMs, 0L);
		long phaseRemainingMs = open
			? Math.max(openWindowEndsAtEpochMs - nowEpochMs, 0L)
			: countdownMs;

		return new RoundWindow(
			nowEpochMs,
			currentRoundStartEpochMs,
			previousRoundStartEpochMs,
			nextRoundStartEpochMs,
			openWindowEndsAtEpochMs,
			countdownMs,
			phaseRemainingMs,
			open);
	}

	private String toRoundLabel(long epochMs) {
		return ROUND_LABEL_FORMATTER.format(Instant.ofEpochMilli(epochMs));
	}

	private LocalDateTime toLocalDateTime(long epochMs) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
	}

	private long toEpochMs(LocalDateTime value) {
		return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public record ClickRecord(
		String userId,
		Long couponId,
		String couponName,
		long reactionTimeMs,
		long clickedAtEpochMs
	) {
	}

	private record RoundWindow(
		long nowEpochMs,
		long currentRoundStartEpochMs,
		long previousRoundStartEpochMs,
		long nextRoundStartEpochMs,
		long openWindowEndsAtEpochMs,
		long countdownMs,
		long phaseRemainingMs,
		boolean isOpen
	) {
	}
}
