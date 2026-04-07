package com.example.waitingsystem.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.example.waitingsystem.common.exception.BusyException;
import com.example.waitingsystem.common.exception.ConflictException;
import com.example.waitingsystem.common.exception.NotFoundException;
import com.example.waitingsystem.config.DemoProperties;
import com.example.waitingsystem.config.QueueProperties;
import com.example.waitingsystem.domain.QueueEntry;
import com.example.waitingsystem.domain.QueueEntryStatus;
import com.example.waitingsystem.domain.QueueEvent;
import com.example.waitingsystem.dto.QueueAdmissionConsumeRequest;
import com.example.waitingsystem.dto.QueueAdmissionConsumeResponse;
import com.example.waitingsystem.dto.QueueAdmissionRequest;
import com.example.waitingsystem.dto.QueueAdmissionResponse;
import com.example.waitingsystem.dto.QueueAdmissionUserResponse;
import com.example.waitingsystem.dto.QueueEnterRequest;
import com.example.waitingsystem.dto.QueueEnterResponse;
import com.example.waitingsystem.dto.QueueStatusResponse;
import com.example.waitingsystem.repository.QueueRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class QueueService {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final Duration ENTER_GUARD_TTL = Duration.ofSeconds(5);
	private static final Duration ADMIT_LOCK_TTL = Duration.ofSeconds(2);

	private final QueueRepository queueRepository;
	private final StringRedisTemplate stringRedisTemplate;
	private final QueueProperties queueProperties;
	private final DemoProperties demoProperties;
	private final TrafficMetricsService trafficMetricsService;
	private final Clock clock;

	public QueueService(
		QueueRepository queueRepository,
		StringRedisTemplate stringRedisTemplate,
		QueueProperties queueProperties,
		DemoProperties demoProperties,
		TrafficMetricsService trafficMetricsService,
		Clock clock
	) {
		this.queueRepository = queueRepository;
		this.stringRedisTemplate = stringRedisTemplate;
		this.queueProperties = queueProperties;
		this.demoProperties = demoProperties;
		this.trafficMetricsService = trafficMetricsService;
		this.clock = clock;
	}

	public QueueEnterResponse enterQueue(QueueEnterRequest request) {
		ensureDemoEventOpen(request.eventId());
		QueueEvent event = getActiveEvent(request.eventId());
		QueueEntry cachedEntry = getRedisEntry(request.eventId(), request.userId());
		if (cachedEntry != null) {
			return toEnterResponse(cachedEntry);
		}

		QueueEntry existingEntry = queueRepository.findEntryByEventIdAndUserId(request.eventId(), request.userId());
		if (existingEntry != null) {
			existingEntry = expireAdmissionIfNeeded(existingEntry);
			if (QueueEntryStatus.EXPIRED.name().equals(existingEntry.getStatus())
				|| QueueEntryStatus.ENTERED.name().equals(existingEntry.getStatus())) {
				return reenterQueue(event, request.userId(), existingEntry);
			}
			saveSnapshot(existingEntry);
			return toEnterResponse(existingEntry);
		}

		String guardKey = enterGuardKey(request.eventId(), request.userId());
		Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(guardKey, "1", ENTER_GUARD_TTL);
		if (!Boolean.TRUE.equals(locked)) {
			throw new BusyException("동일한 사용자 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요.");
		}

		try {
			QueueEntry duplicatedEntry = queueRepository.findEntryByEventIdAndUserId(request.eventId(), request.userId());
			if (duplicatedEntry != null) {
				duplicatedEntry = expireAdmissionIfNeeded(duplicatedEntry);
				if (QueueEntryStatus.EXPIRED.name().equals(duplicatedEntry.getStatus())
					|| QueueEntryStatus.ENTERED.name().equals(duplicatedEntry.getStatus())) {
					return reenterQueue(event, request.userId(), duplicatedEntry);
				}
				saveSnapshot(duplicatedEntry);
				return toEnterResponse(duplicatedEntry);
			}

			Long queueSequence = stringRedisTemplate.opsForValue().increment(queueSequenceKey(request.eventId()));
			LocalDateTime now = LocalDateTime.now(clock);
			QueueEntry queueEntry = new QueueEntry();
			queueEntry.setEventId(event.getEventId());
			queueEntry.setUserId(request.userId());
			queueEntry.setQueueToken(UUID.randomUUID().toString().replace("-", ""));
			queueEntry.setQueueSequence(queueSequence);
			queueEntry.setStatus(QueueEntryStatus.WAITING.name());
			queueEntry.setEnteredAt(now);
			queueEntry.setCreatedAt(now);
			queueEntry.setUpdatedAt(now);

			queueRepository.insertQueueEntry(queueEntry);
			saveSnapshot(queueEntry);
			return toEnterResponse(queueEntry);
		} catch (DataIntegrityViolationException ex) {
			QueueEntry duplicatedEntry = queueRepository.findEntryByEventIdAndUserId(request.eventId(), request.userId());
			if (duplicatedEntry != null) {
				return toEnterResponse(duplicatedEntry);
			}
			throw new ConflictException("이미 대기열에 등록된 사용자입니다.", ex);
		} finally {
			stringRedisTemplate.delete(guardKey);
		}
	}

	public QueueStatusResponse getStatus(Long eventId, String userId) {
		QueueEntry queueEntry = getRedisEntry(eventId, userId);
		if (queueEntry == null) {
			queueEntry = queueRepository.findEntryByEventIdAndUserId(eventId, userId);
			if (queueEntry == null) {
				throw new NotFoundException("대기열 정보를 찾을 수 없습니다.");
			}
			queueEntry = expireAdmissionIfNeeded(queueEntry);
			saveSnapshot(queueEntry);
		}

		return toStatusResponse(queueEntry);
	}

	public QueueStatusResponse getStatusOrNull(Long eventId, String userId) {
		try {
			return getStatus(eventId, userId);
		} catch (NotFoundException ex) {
			return null;
		}
	}

	public long getWaitingCount(Long eventId) {
		Long waitingCount = stringRedisTemplate.opsForZSet().zCard(waitingQueueKey(eventId));
		long resolvedCount = waitingCount == null ? 0L : waitingCount;
		trafficMetricsService.updateQueueDepth(eventId, resolvedCount);
		return resolvedCount;
	}

	public QueueAdmissionResponse admitNextBatch(QueueAdmissionRequest request) {
		long startedAt = System.nanoTime();
		int admittedCount = 0;
		QueueEvent event = getActiveEvent(request.eventId());
		int requestedCount = request.count() == null || request.count() <= 0
			? resolveAdmissionBatchSize(event)
			: request.count();

		String lockKey = admitLockKey(request.eventId());
		Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", ADMIT_LOCK_TTL);
		if (!Boolean.TRUE.equals(locked)) {
			throw new BusyException("현재 입장 처리 배치가 실행 중입니다.");
		}

		try {
			List<String> userIds = stringRedisTemplate.opsForZSet().range(waitingQueueKey(request.eventId()), 0, requestedCount - 1)
				.stream()
				.toList();
			List<QueueAdmissionUserResponse> admittedUsers = new ArrayList<>();

			for (String userId : userIds) {
				QueueEntry queueEntry = queueRepository.findEntryByEventIdAndUserId(request.eventId(), userId);
				if (queueEntry == null || !QueueEntryStatus.WAITING.name().equals(queueEntry.getStatus())) {
					removeWaitingState(request.eventId(), userId);
					continue;
				}

				LocalDateTime admittedAt = LocalDateTime.now(clock);
				LocalDateTime expiresAt = admittedAt.plusSeconds(resolveAdmissionWindowSeconds(event));
				String admissionToken = UUID.randomUUID().toString().replace("-", "");

				int updated = queueRepository.updateEntryAsAdmitted(
					request.eventId(),
					userId,
					admissionToken,
					admittedAt,
					expiresAt,
					admittedAt);
				if (updated != 1) {
					continue;
				}

				queueEntry.setStatus(QueueEntryStatus.ADMITTED.name());
				queueEntry.setAdmissionToken(admissionToken);
				queueEntry.setAdmittedAt(admittedAt);
				queueEntry.setAdmissionExpiresAt(expiresAt);
				queueEntry.setUpdatedAt(admittedAt);

				saveSnapshot(queueEntry);
				stringRedisTemplate.opsForZSet().remove(waitingQueueKey(request.eventId()), userId);

				admittedUsers.add(new QueueAdmissionUserResponse(
					userId,
					queueEntry.getQueueToken(),
					admissionToken,
					expiresAt));
			}

			admittedCount = admittedUsers.size();
			return new QueueAdmissionResponse(
				request.eventId(),
				requestedCount,
				admittedCount,
				admittedUsers);
		} finally {
			long durationMs = Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
			trafficMetricsService.recordQueueAdmit(request.eventId(), requestedCount, admittedCount, durationMs);
			trafficMetricsService.updateQueueDepth(request.eventId(), getWaitingCount(request.eventId()));
			stringRedisTemplate.delete(lockKey);
		}
	}

	public QueueAdmissionConsumeResponse consumeAdmission(QueueAdmissionConsumeRequest request) {
		QueueEntry queueEntry = queueRepository.findEntryByAdmissionToken(request.eventId(), request.admissionToken());
		if (queueEntry == null || !QueueEntryStatus.ADMITTED.name().equals(queueEntry.getStatus())) {
			throw new NotFoundException("유효한 입장 토큰이 없습니다.");
		}

		queueEntry = expireAdmissionIfNeeded(queueEntry);
		if (!QueueEntryStatus.ADMITTED.name().equals(queueEntry.getStatus())) {
			throw new ConflictException("입장 토큰이 만료되었습니다.");
		}

		LocalDateTime now = LocalDateTime.now(clock);
		int updated = queueRepository.updateEntryAsEntered(request.eventId(), request.admissionToken(), now);
		if (updated != 1) {
			throw new ConflictException("입장 처리에 실패했습니다.");
		}

		queueEntry.setStatus(QueueEntryStatus.ENTERED.name());
		queueEntry.setUpdatedAt(now);
		saveSnapshot(queueEntry);

		return new QueueAdmissionConsumeResponse(
			request.eventId(),
			queueEntry.getUserId(),
			QueueEntryStatus.ENTERED.name(),
			now);
	}

	public List<QueueEvent> findActiveEvents() {
		ensureDemoEventOpen(demoProperties.getDefaultEventId());
		return queueRepository.findActiveEvents();
	}

	public void resetUserQueueForDemo(Long eventId, String userId) {
		QueueEntry existingEntry = queueRepository.findEntryByEventIdAndUserId(eventId, userId);
		if (existingEntry == null) {
			clearRedisQueueState(eventId, userId, null);
			return;
		}

		clearRedisQueueState(eventId, userId, existingEntry.getAdmissionToken());
		queueRepository.resetEntryForDemo(eventId, userId, LocalDateTime.now(clock));
	}

	public void resetCrowdQueueForDemo(Long eventId, String userPrefix) {
		if (userPrefix == null || userPrefix.isBlank()) {
			return;
		}

		Set<String> candidateUserIds = new HashSet<>();
		Set<String> waitingUserIds = stringRedisTemplate.opsForZSet().range(waitingQueueKey(eventId), 0, -1);
		if (waitingUserIds != null) {
			waitingUserIds.stream()
				.filter(userId -> userId != null && userId.startsWith(userPrefix))
				.forEach(candidateUserIds::add);
		}

		Set<String> entryKeys = stringRedisTemplate.keys(queueEntryKeyPattern(eventId, userPrefix));
		if (entryKeys != null) {
			for (String entryKey : entryKeys) {
				String userId = extractUserIdFromEntryKey(eventId, entryKey);
				if (userId != null && userId.startsWith(userPrefix)) {
					Map<Object, Object> values = stringRedisTemplate.opsForHash().entries(entryKey);
					String admissionToken = stringValue(values.get("admissionToken"));
					clearRedisQueueState(eventId, userId, admissionToken);
					candidateUserIds.add(userId);
				}
			}
		}

		for (String userId : candidateUserIds) {
			clearRedisQueueState(eventId, userId, null);
		}

		queueRepository.resetEntriesForDemoByUserPrefix(eventId, userPrefix + "%", LocalDateTime.now(clock));
	}

	private QueueEvent getActiveEvent(Long eventId) {
		QueueEvent queueEvent = queueRepository.findActiveEventById(eventId);
		if (queueEvent == null) {
			throw new NotFoundException("오픈된 대기열 이벤트가 없습니다.");
		}
		return queueEvent;
	}

	private void ensureDemoEventOpen(Long eventId) {
		if (eventId == null || eventId.longValue() != demoProperties.getDefaultEventId()) {
			return;
		}

		QueueEvent activeEvent = queueRepository.findActiveEventById(eventId);
		if (activeEvent != null) {
			return;
		}

		LocalDateTime now = LocalDateTime.now(clock);
		QueueEvent queueEvent = queueRepository.findEventById(eventId);
		if (queueEvent == null) {
			queueEvent = new QueueEvent();
			queueEvent.setEventId(eventId);
			queueEvent.setCreatedAt(now);
		}

		queueEvent.setEventName("DEMO_COUPON_OPEN");
		queueEvent.setCapacityPerSecond(Math.max(1, demoProperties.getAdmissionBatchSize()));
		queueEvent.setAdmissionWindowSeconds(Math.max(30, demoProperties.getAdmissionWindowSeconds()));
		queueEvent.setEventStatus("OPEN");
		queueEvent.setActiveYn("Y");
		queueEvent.setStartsAt(now.minusMinutes(30));
		queueEvent.setEndsAt(now.plusDays(30));
		queueEvent.setUpdatedAt(now);
		queueRepository.upsertDemoEvent(queueEvent);
	}

	private QueueEnterResponse reenterQueue(QueueEvent event, String userId, QueueEntry existingEntry) {
		Long queueSequence = stringRedisTemplate.opsForValue().increment(queueSequenceKey(event.getEventId()));
		LocalDateTime now = LocalDateTime.now(clock);
		String queueToken = UUID.randomUUID().toString().replace("-", "");

		int updated = queueRepository.updateEntryForRejoin(
			event.getEventId(),
			userId,
			queueToken,
			queueSequence,
			now,
			now);

		if (updated != 1) {
			throw new ConflictException("대기열 재진입 처리에 실패했습니다. 다시 시도해 주세요.");
		}

		existingEntry.setQueueToken(queueToken);
		existingEntry.setQueueSequence(queueSequence);
		existingEntry.setStatus(QueueEntryStatus.WAITING.name());
		existingEntry.setEnteredAt(now);
		existingEntry.setAdmissionToken(null);
		existingEntry.setAdmittedAt(null);
		existingEntry.setAdmissionExpiresAt(null);
		existingEntry.setUpdatedAt(now);
		saveSnapshot(existingEntry);
		return toEnterResponse(existingEntry);
	}

	private QueueEntry getRedisEntry(Long eventId, String userId) {
		Map<Object, Object> values = stringRedisTemplate.opsForHash().entries(queueEntryKey(eventId, userId));
		if (values == null || values.isEmpty()) {
			return null;
		}

		QueueEntry queueEntry = new QueueEntry();
		queueEntry.setEventId(eventId);
		queueEntry.setUserId(userId);
		queueEntry.setQueueToken(stringValue(values.get("queueToken")));
		queueEntry.setQueueSequence(longValue(values.get("queueSequence")));
		queueEntry.setStatus(stringValue(values.get("status")));
		queueEntry.setEnteredAt(dateTimeValue(values.get("enteredAt")));
		queueEntry.setAdmissionToken(stringValue(values.get("admissionToken")));
		queueEntry.setAdmittedAt(dateTimeValue(values.get("admittedAt")));
		queueEntry.setAdmissionExpiresAt(dateTimeValue(values.get("admissionExpiresAt")));
		return expireAdmissionIfNeeded(queueEntry);
	}

	private void saveSnapshot(QueueEntry queueEntry) {
		String entryKey = queueEntryKey(queueEntry.getEventId(), queueEntry.getUserId());
		Map<String, String> values = new LinkedHashMap<>();
		values.put("queueToken", nullSafe(queueEntry.getQueueToken()));
		values.put("queueSequence", queueEntry.getQueueSequence() == null ? "" : String.valueOf(queueEntry.getQueueSequence()));
		values.put("status", nullSafe(queueEntry.getStatus()));
		values.put("enteredAt", format(queueEntry.getEnteredAt()));
		values.put("admissionToken", nullSafe(queueEntry.getAdmissionToken()));
		values.put("admittedAt", format(queueEntry.getAdmittedAt()));
		values.put("admissionExpiresAt", format(queueEntry.getAdmissionExpiresAt()));
		stringRedisTemplate.opsForHash().putAll(entryKey, values);

		if (QueueEntryStatus.WAITING.name().equals(queueEntry.getStatus())) {
			stringRedisTemplate.opsForZSet().add(
				waitingQueueKey(queueEntry.getEventId()),
				queueEntry.getUserId(),
				queueEntry.getQueueSequence() == null ? 0D : queueEntry.getQueueSequence().doubleValue());
			if (queueEntry.getAdmissionToken() != null) {
				stringRedisTemplate.delete(admissionTokenKey(queueEntry.getEventId(), queueEntry.getAdmissionToken()));
			}
			return;
		}

		stringRedisTemplate.opsForZSet().remove(waitingQueueKey(queueEntry.getEventId()), queueEntry.getUserId());
		if (QueueEntryStatus.ADMITTED.name().equals(queueEntry.getStatus())
			&& queueEntry.getAdmissionToken() != null
			&& queueEntry.getAdmissionExpiresAt() != null) {
			long seconds = Duration.between(LocalDateTime.now(clock), queueEntry.getAdmissionExpiresAt()).getSeconds();
			if (seconds > 0) {
				stringRedisTemplate.opsForValue().set(
					admissionTokenKey(queueEntry.getEventId(), queueEntry.getAdmissionToken()),
					queueEntry.getUserId(),
					Duration.ofSeconds(seconds));
			}
		} else if (queueEntry.getAdmissionToken() != null) {
			stringRedisTemplate.delete(admissionTokenKey(queueEntry.getEventId(), queueEntry.getAdmissionToken()));
		}
	}

	private QueueEntry expireAdmissionIfNeeded(QueueEntry queueEntry) {
		if (queueEntry == null) {
			return null;
		}

		if (!QueueEntryStatus.ADMITTED.name().equals(queueEntry.getStatus())) {
			return queueEntry;
		}

		LocalDateTime expiresAt = queueEntry.getAdmissionExpiresAt();
		if (expiresAt == null || expiresAt.isAfter(LocalDateTime.now(clock))) {
			return queueEntry;
		}

		queueRepository.updateEntryAsExpired(queueEntry.getEventId(), queueEntry.getUserId(), LocalDateTime.now(clock));
		queueEntry.setStatus(QueueEntryStatus.EXPIRED.name());
		queueEntry.setUpdatedAt(LocalDateTime.now(clock));
		saveSnapshot(queueEntry);
		return queueEntry;
	}

	private QueueEnterResponse toEnterResponse(QueueEntry queueEntry) {
		QueueStatusResponse statusResponse = toStatusResponse(queueEntry);
		return new QueueEnterResponse(
			statusResponse.eventId(),
			statusResponse.userId(),
			statusResponse.status(),
			statusResponse.queueToken(),
			statusResponse.queueNumber(),
			statusResponse.currentPosition(),
			statusResponse.aheadCount(),
			statusResponse.enteredAt(),
			statusResponse.admissionToken(),
			statusResponse.admissionExpiresAt());
	}

	private QueueStatusResponse toStatusResponse(QueueEntry queueEntry) {
		Long currentPosition = null;
		Long aheadCount = null;

		if (QueueEntryStatus.WAITING.name().equals(queueEntry.getStatus())) {
			Long rank = stringRedisTemplate.opsForZSet().rank(waitingQueueKey(queueEntry.getEventId()), queueEntry.getUserId());
			if (rank != null) {
				currentPosition = rank + 1;
				aheadCount = rank;
			}
		}

		return new QueueStatusResponse(
			queueEntry.getEventId(),
			queueEntry.getUserId(),
			queueEntry.getStatus(),
			queueEntry.getQueueToken(),
			queueEntry.getQueueSequence(),
			currentPosition,
			aheadCount,
			queueEntry.getEnteredAt(),
			queueEntry.getAdmissionToken(),
			queueEntry.getAdmittedAt(),
			queueEntry.getAdmissionExpiresAt());
	}

	private int resolveAdmissionBatchSize(QueueEvent queueEvent) {
		if (queueEvent.getEventId() != null && queueEvent.getEventId() == demoProperties.getDefaultEventId()) {
			return Math.max(1, demoProperties.getAdmissionBatchSize());
		}

		return queueEvent.getCapacityPerSecond() == null || queueEvent.getCapacityPerSecond() <= 0
			? queueProperties.getDefaultAdmissionBatchSize()
			: queueEvent.getCapacityPerSecond();
	}

	private int resolveAdmissionWindowSeconds(QueueEvent queueEvent) {
		if (queueEvent.getEventId() != null && queueEvent.getEventId() == demoProperties.getDefaultEventId()) {
			return Math.max(30, demoProperties.getAdmissionWindowSeconds());
		}

		return queueEvent.getAdmissionWindowSeconds() == null || queueEvent.getAdmissionWindowSeconds() <= 0
			? queueProperties.getDefaultAdmissionWindowSeconds()
			: queueEvent.getAdmissionWindowSeconds();
	}

	private void removeWaitingState(Long eventId, String userId) {
		clearRedisQueueState(eventId, userId, null);
	}

	private void clearRedisQueueState(Long eventId, String userId, String admissionToken) {
		stringRedisTemplate.opsForZSet().remove(waitingQueueKey(eventId), userId);
		stringRedisTemplate.delete(queueEntryKey(eventId, userId));
		if (admissionToken != null && !admissionToken.isBlank()) {
			stringRedisTemplate.delete(admissionTokenKey(eventId, admissionToken));
		}
	}

	private String waitingQueueKey(Long eventId) {
		return "queue:waiting:" + eventId;
	}

	private String queueSequenceKey(Long eventId) {
		return "queue:sequence:" + eventId;
	}

	private String queueEntryKey(Long eventId, String userId) {
		return "queue:entry:" + eventId + ":" + userId;
	}

	private String queueEntryKeyPattern(Long eventId, String userPrefix) {
		return "queue:entry:" + eventId + ":" + userPrefix + "*";
	}

	private String enterGuardKey(Long eventId, String userId) {
		return "queue:guard:" + eventId + ":" + userId;
	}

	private String admitLockKey(Long eventId) {
		return "queue:admit:lock:" + eventId;
	}

	private String admissionTokenKey(Long eventId, String admissionToken) {
		return "queue:admission:" + eventId + ":" + admissionToken;
	}

	private String extractUserIdFromEntryKey(Long eventId, String entryKey) {
		String prefix = "queue:entry:" + eventId + ":";
		if (entryKey == null || !entryKey.startsWith(prefix)) {
			return null;
		}

		return entryKey.substring(prefix.length());
	}

	private String nullSafe(String value) {
		return value == null ? "" : value;
	}

	private String format(LocalDateTime value) {
		return value == null ? "" : value.format(DATE_TIME_FORMATTER);
	}

	private String stringValue(Object value) {
		if (value == null) {
			return null;
		}

		String parsed = String.valueOf(value);
		return parsed.isBlank() ? null : parsed;
	}

	private Long longValue(Object value) {
		String parsed = stringValue(value);
		return parsed == null ? null : Long.parseLong(parsed);
	}

	private LocalDateTime dateTimeValue(Object value) {
		String parsed = stringValue(value);
		return parsed == null ? null : LocalDateTime.parse(parsed, DATE_TIME_FORMATTER);
	}
}
