package com.example.waitingsystem.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class TrafficMetricsService {

	private final MeterRegistry meterRegistry;
	private final ConcurrentMap<Long, AtomicLong> queueDepthGauges = new ConcurrentHashMap<>();
	private final ConcurrentMap<Long, AtomicLong> couponRemainingGauges = new ConcurrentHashMap<>();

	public TrafficMetricsService(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void recordQueueEnter(Long eventId, String outcome, long durationMs) {
		Counter.builder("waiting.queue.enter.requests")
			.tag("eventId", String.valueOf(eventId))
			.tag("outcome", outcome)
			.register(meterRegistry)
			.increment();

		Timer.builder("waiting.queue.enter.duration")
			.tag("eventId", String.valueOf(eventId))
			.tag("outcome", outcome)
			.register(meterRegistry)
			.record(durationMs, TimeUnit.MILLISECONDS);
	}

	public void recordQueueAdmit(Long eventId, int requestedCount, int admittedCount, long durationMs) {
		Counter.builder("waiting.queue.admit.batches")
			.tag("eventId", String.valueOf(eventId))
			.register(meterRegistry)
			.increment();

		DistributionSummary.builder("waiting.queue.admit.requested")
			.tag("eventId", String.valueOf(eventId))
			.register(meterRegistry)
			.record(requestedCount);

		DistributionSummary.builder("waiting.queue.admit.admitted")
			.tag("eventId", String.valueOf(eventId))
			.register(meterRegistry)
			.record(admittedCount);

		Timer.builder("waiting.queue.admit.duration")
			.tag("eventId", String.valueOf(eventId))
			.register(meterRegistry)
			.record(durationMs, TimeUnit.MILLISECONDS);
	}

	public void updateQueueDepth(Long eventId, long queueDepth) {
		AtomicLong gaugeValue = queueDepthGauges.computeIfAbsent(eventId, id -> registerQueueDepthGauge(id));
		gaugeValue.set(queueDepth);
	}

	public void recordCouponIssue(Long couponId, String outcome, long durationMs) {
		Counter.builder("waiting.coupon.issue.requests")
			.tag("couponId", String.valueOf(couponId))
			.tag("outcome", outcome)
			.register(meterRegistry)
			.increment();

		Timer.builder("waiting.coupon.issue.duration")
			.tag("couponId", String.valueOf(couponId))
			.tag("outcome", outcome)
			.register(meterRegistry)
			.record(durationMs, TimeUnit.MILLISECONDS);
	}

	public void updateCouponRemaining(Long couponId, Number remainingCount) {
		if (remainingCount == null) {
			return;
		}

		AtomicLong gaugeValue = couponRemainingGauges.computeIfAbsent(couponId, id -> registerCouponRemainingGauge(id));
		gaugeValue.set(remainingCount.longValue());
	}

	private AtomicLong registerQueueDepthGauge(Long eventId) {
		AtomicLong gaugeValue = new AtomicLong(0L);
		Gauge.builder("waiting.queue.depth", gaugeValue, AtomicLong::get)
			.tag("eventId", String.valueOf(eventId))
			.register(meterRegistry);
		return gaugeValue;
	}

	private AtomicLong registerCouponRemainingGauge(Long couponId) {
		AtomicLong gaugeValue = new AtomicLong(0L);
		Gauge.builder("waiting.coupon.remaining", gaugeValue, AtomicLong::get)
			.tag("couponId", String.valueOf(couponId))
			.register(meterRegistry);
		return gaugeValue;
	}
}
