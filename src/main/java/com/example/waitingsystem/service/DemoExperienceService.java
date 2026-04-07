package com.example.waitingsystem.service;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import com.example.waitingsystem.config.DemoProperties;
import com.example.waitingsystem.config.QueueProperties;
import com.example.waitingsystem.domain.CouponStock;
import com.example.waitingsystem.dto.DemoJoinRequest;
import com.example.waitingsystem.dto.DemoMonitorResponse;
import com.example.waitingsystem.dto.QueueEnterRequest;
import com.example.waitingsystem.dto.QueueStatusResponse;
import org.springframework.stereotype.Service;

@Service
public class DemoExperienceService {

	private final QueueService queueService;
	private final CouponService couponService;
	private final DemoProperties demoProperties;
	private final QueueProperties queueProperties;
	private final DemoRoundService demoRoundService;
	private final DemoTrafficStateService demoTrafficStateService;
	private final Clock clock;

	public DemoExperienceService(
		QueueService queueService,
		CouponService couponService,
		DemoProperties demoProperties,
		QueueProperties queueProperties,
		DemoRoundService demoRoundService,
		DemoTrafficStateService demoTrafficStateService,
		Clock clock
	) {
		this.queueService = queueService;
		this.couponService = couponService;
		this.demoProperties = demoProperties;
		this.queueProperties = queueProperties;
		this.demoRoundService = demoRoundService;
		this.demoTrafficStateService = demoTrafficStateService;
		this.clock = clock;
	}

	public DemoMonitorResponse joinExperience(DemoJoinRequest request) {
		Long eventId = request.eventId() == null ? demoProperties.getDefaultEventId() : request.eventId();
		Long couponId = request.couponId() == null ? demoProperties.getDefaultCouponId() : request.couponId();
		String userId = request.userId() == null || request.userId().isBlank()
			? "demo-user-" + Instant.now(clock).toEpochMilli()
			: request.userId().trim();
		demoTrafficStateService.saveActiveCouponId(eventId, couponId);
		CouponStock selectedCoupon = couponService.getCouponStock(couponId);
		String couponName = selectedCoupon == null ? "Coupon" : selectedCoupon.getCouponName();
		DemoRoundService.ClickRecord clickRecord = demoRoundService.recordClick(userId, couponId, couponName);
		queueService.resetUserQueueForDemo(eventId, userId);

		int simulatedUsers = 0;
		int usersAfterCurrent = 0;
		if (demoProperties.isTrafficSimulationEnabled()) {
			queueService.resetCrowdQueueForDemo(eventId, demoProperties.getFakeUserPrefix());
			int burstSize = request.burstSize() == null || request.burstSize() <= 0
				? demoProperties.getTrafficBurstSize()
				: request.burstSize();
			TrafficPlan trafficPlan = buildTrafficPlan(burstSize);
			simulatedUsers = trafficPlan.totalUsers();
			usersAfterCurrent = trafficPlan.usersAfterCurrent();
			simulateUsersBeforeJoin(eventId, userId, trafficPlan.usersBeforeCurrent());
		}

		queueService.enterQueue(new QueueEnterRequest(eventId, userId));
		simulateUsersAfterJoin(eventId, userId, usersAfterCurrent);
		QueueStatusResponse currentStatus = queueService.getStatusOrNull(eventId, userId);

		return buildResponse(eventId, couponId, userId, currentStatus, simulatedUsers, clickRecord);
	}

	public DemoMonitorResponse getMonitor(Long eventId, Long couponId, String userId) {
		Long resolvedEventId = eventId == null ? demoProperties.getDefaultEventId() : eventId;
		Long resolvedCouponId = couponId == null ? demoProperties.getDefaultCouponId() : couponId;
		QueueStatusResponse status = queueService.getStatusOrNull(resolvedEventId, userId);
		return buildResponse(resolvedEventId, resolvedCouponId, userId, status, 0, null);
	}

	private TrafficPlan buildTrafficPlan(int burstSize) {
		int normalizedBurstSize = Math.max(0, burstSize);
		if (normalizedBurstSize == 0) {
			return new TrafficPlan(0, 0);
		}

		int maxPossibleBeforeCurrent = Math.max(normalizedBurstSize - 1, 0);
		int preferredMinBeforeCurrent = (int) Math.ceil(normalizedBurstSize * 0.6d);
		int preferredMaxBeforeCurrent = (int) Math.ceil(normalizedBurstSize * 0.85d);
		int minBeforeCurrent = Math.min(
			Math.max(Math.max(0, demoProperties.getMinUsersBeforeCurrent()), preferredMinBeforeCurrent),
			maxPossibleBeforeCurrent);
		int maxBeforeCurrent = Math.min(
			Math.max(Math.max(minBeforeCurrent, demoProperties.getMaxUsersBeforeCurrent()), preferredMaxBeforeCurrent),
			maxPossibleBeforeCurrent);
		int usersBeforeCurrent = maxBeforeCurrent > minBeforeCurrent
			? ThreadLocalRandom.current().nextInt(minBeforeCurrent, maxBeforeCurrent + 1)
			: minBeforeCurrent;
		int usersAfterCurrent = Math.max(0, normalizedBurstSize - usersBeforeCurrent);
		return new TrafficPlan(usersBeforeCurrent, usersAfterCurrent);
	}

	private void simulateUsersBeforeJoin(Long eventId, String currentUserId, int usersBeforeCurrent) {
		if (usersBeforeCurrent <= 0) {
			return;
		}

		String sessionKey = Instant.now(clock).toEpochMilli() + "-" + Math.abs(currentUserId.hashCode());
		for (int index = 0; index < usersBeforeCurrent; index++) {
			queueService.enterQueue(new QueueEnterRequest(eventId, buildFakeUserId(sessionKey, "before", index)));
		}
	}

	private void simulateUsersAfterJoin(Long eventId, String currentUserId, int usersAfterCurrent) {
		if (usersAfterCurrent <= 0) {
			return;
		}

		String sessionKey = Instant.now(clock).toEpochMilli() + "-" + Math.abs(currentUserId.hashCode());

		for (int index = 0; index < usersAfterCurrent; index++) {
			queueService.enterQueue(new QueueEnterRequest(eventId, buildFakeUserId(sessionKey, "after", index)));
		}
	}

	private String buildFakeUserId(String sessionKey, String phase, int index) {
		return demoProperties.getFakeUserPrefix() + "-" + sessionKey + "-" + phase + "-" + index;
	}

	private DemoMonitorResponse buildResponse(
		Long eventId,
		Long couponId,
		String userId,
		QueueStatusResponse status,
		int simulatedUsers,
		DemoRoundService.ClickRecord clickRecord
	) {
		CouponStock couponStock = couponService.getCouponStock(couponId);
		DemoRoundService.ClickRecord resolvedClickRecord = clickRecord == null
			? demoRoundService.findLatestClickRecord(userId)
			: clickRecord;
		long queueLength = queueService.getWaitingCount(eventId);
		int admissionBatchSize = resolveAdmissionBatchSize(eventId);
		long schedulerDelayMs = Math.max(queueProperties.getScheduler().getFixedDelayMs(), 1L);
		long estimatedWaitSeconds = estimateWaitSeconds(status, admissionBatchSize, schedulerDelayMs);
		String congestionLevel = resolveCongestionLevel(queueLength, admissionBatchSize);

		return new DemoMonitorResponse(
			eventId,
			couponId,
			couponStock == null ? null : couponStock.getCouponName(),
			userId,
			queueLength,
			estimatedWaitSeconds,
			admissionBatchSize,
			schedulerDelayMs,
			congestionLevel,
			status == null ? null : status.status(),
			status == null ? null : status.queueToken(),
			status == null ? null : status.queueNumber(),
			status == null ? null : status.currentPosition(),
			status == null ? null : status.aheadCount(),
			status == null ? null : status.admissionToken(),
			status == null ? null : status.admissionExpiresAt(),
			couponStock == null ? null : couponStock.getRemainCount(),
			couponStock == null ? null : couponStock.getTotalCount(),
			couponStock == null ? null : couponStock.getStatus(),
			simulatedUsers,
			resolvedClickRecord == null ? null : resolvedClickRecord.reactionTimeMs(),
			resolvedClickRecord == null ? null : resolvedClickRecord.clickedAtEpochMs());
	}

	private int resolveAdmissionBatchSize(Long eventId) {
		if (eventId != null && eventId == demoProperties.getDefaultEventId()) {
			return Math.max(1, demoProperties.getAdmissionBatchSize());
		}

		return Math.max(1, queueProperties.getDefaultAdmissionBatchSize());
	}

	private long estimateWaitSeconds(QueueStatusResponse status, int admissionBatchSize, long schedulerDelayMs) {
		if (status == null || status.currentPosition() == null || status.currentPosition() <= 0) {
			return 0L;
		}

		if (!"WAITING".equals(status.status())) {
			return 0L;
		}

		long cycles = Math.max(1L, (long) Math.ceil((double) status.currentPosition() / admissionBatchSize));
		return Math.max(1L, (cycles * schedulerDelayMs) / 1000L);
	}

	private String resolveCongestionLevel(long queueLength, int admissionBatchSize) {
		long lowThreshold = admissionBatchSize * 3L;
		long mediumThreshold = admissionBatchSize * 8L;

		if (queueLength <= lowThreshold) {
			return "LOW";
		}

		if (queueLength <= mediumThreshold) {
			return "MEDIUM";
		}

		return "HIGH";
	}

	private record TrafficPlan(int usersBeforeCurrent, int usersAfterCurrent) {
		int totalUsers() {
			return usersBeforeCurrent + usersAfterCurrent;
		}
	}
}
