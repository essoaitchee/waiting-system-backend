package com.example.waitingsystem.service;

import java.util.List;

import com.example.waitingsystem.common.exception.ConflictException;
import com.example.waitingsystem.common.exception.NotFoundException;
import com.example.waitingsystem.common.exception.SoldOutException;
import com.example.waitingsystem.config.DemoProperties;
import com.example.waitingsystem.config.QueueProperties;
import com.example.waitingsystem.domain.QueueEvent;
import com.example.waitingsystem.dto.CouponIssueRequest;
import com.example.waitingsystem.dto.QueueAdmissionConsumeRequest;
import com.example.waitingsystem.dto.QueueAdmissionRequest;
import com.example.waitingsystem.dto.QueueAdmissionResponse;
import com.example.waitingsystem.dto.QueueAdmissionUserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueAdmissionScheduler {

	private static final Logger log = LoggerFactory.getLogger(QueueAdmissionScheduler.class);

	private final QueueService queueService;
	private final QueueProperties queueProperties;
	private final DemoProperties demoProperties;
	private final DemoTrafficStateService demoTrafficStateService;
	private final CouponService couponService;

	public QueueAdmissionScheduler(
		QueueService queueService,
		QueueProperties queueProperties,
		DemoProperties demoProperties,
		DemoTrafficStateService demoTrafficStateService,
		CouponService couponService
	) {
		this.queueService = queueService;
		this.queueProperties = queueProperties;
		this.demoProperties = demoProperties;
		this.demoTrafficStateService = demoTrafficStateService;
		this.couponService = couponService;
	}

	@Scheduled(fixedDelayString = "${app.queue.scheduler.fixed-delay-ms:1000}")
	public void admitWaitingUsers() {
		if (!queueProperties.getScheduler().isEnabled()) {
			return;
		}

		List<QueueEvent> activeEvents = queueService.findActiveEvents();
		for (QueueEvent activeEvent : activeEvents) {
			try {
				QueueAdmissionResponse response = queueService.admitNextBatch(new QueueAdmissionRequest(activeEvent.getEventId(), null));
				autoConsumeDemoUsers(activeEvent.getEventId(), response.admittedUsers());
			} catch (Exception ex) {
				log.warn("Queue admission scheduler failed. eventId={}, message={}", activeEvent.getEventId(), ex.getMessage());
			}
		}
	}

	private void autoConsumeDemoUsers(Long eventId, List<QueueAdmissionUserResponse> admittedUsers) {
		if (eventId == null || eventId.longValue() != demoProperties.getDefaultEventId()) {
			return;
		}

		if (admittedUsers == null || admittedUsers.isEmpty()) {
			return;
		}

		Long couponId = demoTrafficStateService.getActiveCouponId(eventId, demoProperties.getDefaultCouponId());
		for (QueueAdmissionUserResponse admittedUser : admittedUsers) {
			String userId = admittedUser.userId();
			if (userId == null || !userId.startsWith(demoProperties.getFakeUserPrefix())) {
				continue;
			}

			try {
				queueService.consumeAdmission(new QueueAdmissionConsumeRequest(eventId, admittedUser.admissionToken()));
				couponService.issueCoupon(new CouponIssueRequest(couponId, userId));
			} catch (SoldOutException ex) {
				log.info("Demo coupon stock sold out while consuming crowd queue. eventId={}, couponId={}", eventId, couponId);
				return;
			} catch (ConflictException | NotFoundException ex) {
				log.debug("Demo crowd user skipped during auto consume. userId={}, message={}", userId, ex.getMessage());
			} catch (Exception ex) {
				log.warn("Demo crowd auto consume failed. userId={}, message={}", userId, ex.getMessage());
			}
		}
	}
}
