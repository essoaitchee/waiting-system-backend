package com.example.waitingsystem.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.example.waitingsystem.common.exception.ConflictException;
import com.example.waitingsystem.common.exception.SoldOutException;
import com.example.waitingsystem.domain.CouponIssue;
import com.example.waitingsystem.domain.CouponStock;
import com.example.waitingsystem.dto.CouponIssueRequest;
import com.example.waitingsystem.dto.CouponIssueResponse;
import com.example.waitingsystem.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

	@Mock
	private CouponRepository couponRepository;

	@Mock
	private TrafficMetricsService trafficMetricsService;

	private CouponService couponService;

	@BeforeEach
	void setUp() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-04-06T12:00:00Z"), ZoneId.of("UTC"));
		couponService = new CouponService(couponRepository, trafficMetricsService, fixedClock);
	}

	@Test
	void issueCouponSucceedsAndUpdatesStockMetric() {
		CouponStock beforeIssue = new CouponStock();
		beforeIssue.setCouponId(201L);
		beforeIssue.setStatus("ACTIVE");
		beforeIssue.setRemainCount(10);

		CouponStock afterIssue = new CouponStock();
		afterIssue.setCouponId(201L);
		afterIssue.setStatus("ACTIVE");
		afterIssue.setRemainCount(9);

		when(couponRepository.findCouponIssueByCouponIdAndUserId(201L, "olive-user")).thenReturn(null);
		when(couponRepository.findCouponStock(201L)).thenReturn(beforeIssue, afterIssue);
		when(couponRepository.decreaseStockIfAvailable(201L)).thenReturn(1);

		CouponIssueResponse response = couponService.issueCoupon(new CouponIssueRequest(201L, "olive-user"));

		assertEquals(201L, response.couponId());
		assertEquals("olive-user", response.userId());
		assertEquals("ISSUED", response.status());
		assertEquals(9, response.remainingCount());
		assertNotNull(response.issuedAt());

		ArgumentCaptor<CouponIssue> couponIssueCaptor = ArgumentCaptor.forClass(CouponIssue.class);
		verify(couponRepository).insertCouponIssue(couponIssueCaptor.capture());
		assertEquals(201L, couponIssueCaptor.getValue().getCouponId());
		assertEquals("olive-user", couponIssueCaptor.getValue().getUserId());

		verify(trafficMetricsService).updateCouponRemaining(201L, 9);
		verify(trafficMetricsService).recordCouponIssue(eq(201L), eq("success"), any(Long.class));
	}

	@Test
	void issueCouponThrowsConflictWhenDuplicateIssueExists() {
		CouponIssue existingIssue = new CouponIssue();
		existingIssue.setCouponId(201L);
		existingIssue.setUserId("olive-user");

		when(couponRepository.findCouponIssueByCouponIdAndUserId(201L, "olive-user")).thenReturn(existingIssue);

		assertThrows(ConflictException.class, () -> couponService.issueCoupon(new CouponIssueRequest(201L, "olive-user")));

		verify(couponRepository, never()).decreaseStockIfAvailable(any());
		verify(couponRepository, never()).insertCouponIssue(any());
		verify(trafficMetricsService).recordCouponIssue(eq(201L), eq("duplicate"), any(Long.class));
	}

	@Test
	void issueCouponThrowsSoldOutWhenStockCannotBeDecreased() {
		CouponStock couponStock = new CouponStock();
		couponStock.setCouponId(201L);
		couponStock.setStatus("ACTIVE");
		couponStock.setRemainCount(0);

		when(couponRepository.findCouponIssueByCouponIdAndUserId(201L, "olive-user")).thenReturn(null);
		when(couponRepository.findCouponStock(201L)).thenReturn(couponStock);
		when(couponRepository.decreaseStockIfAvailable(201L)).thenReturn(0);

		assertThrows(SoldOutException.class, () -> couponService.issueCoupon(new CouponIssueRequest(201L, "olive-user")));

		verify(couponRepository, never()).insertCouponIssue(any());
		verify(trafficMetricsService).recordCouponIssue(eq(201L), eq("sold_out"), any(Long.class));
	}
}
