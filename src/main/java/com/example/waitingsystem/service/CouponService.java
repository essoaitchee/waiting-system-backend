package com.example.waitingsystem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.waitingsystem.common.exception.ConflictException;
import com.example.waitingsystem.common.exception.NotFoundException;
import com.example.waitingsystem.common.exception.SoldOutException;
import com.example.waitingsystem.domain.CouponIssue;
import com.example.waitingsystem.domain.CouponStock;
import com.example.waitingsystem.dto.CouponIssueRequest;
import com.example.waitingsystem.dto.CouponIssueResponse;
import com.example.waitingsystem.dto.CouponResetResponse;
import com.example.waitingsystem.dto.CouponSummaryResponse;
import com.example.waitingsystem.repository.CouponRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

	private final CouponRepository couponRepository;
	private final TrafficMetricsService trafficMetricsService;
	private final Clock clock;

	public CouponService(CouponRepository couponRepository, TrafficMetricsService trafficMetricsService, Clock clock) {
		this.couponRepository = couponRepository;
		this.trafficMetricsService = trafficMetricsService;
		this.clock = clock;
	}

	@Transactional
	public CouponIssueResponse issueCoupon(CouponIssueRequest request) {
		long startedAt = System.nanoTime();
		String outcome = "error";
		String resolvedUserId = request.userId() == null ? "" : request.userId().trim();

		try {
			CouponIssue existingIssue = couponRepository.findCouponIssueByCouponIdAndUserId(request.couponId(), resolvedUserId);
			if (existingIssue != null) {
				outcome = "duplicate";
				throw new ConflictException("이미 발급받은 쿠폰입니다.");
			}

			CouponStock couponStock = couponRepository.findCouponStock(request.couponId());
			if (couponStock == null || !"ACTIVE".equals(couponStock.getStatus())) {
				outcome = "not_found";
				throw new NotFoundException("사용 가능한 쿠폰이 없습니다.");
			}

			int updatedCount = couponRepository.decreaseStockIfAvailable(request.couponId());
			if (updatedCount == 0) {
				outcome = "sold_out";
				throw new SoldOutException("쿠폰이 모두 소진되었습니다.");
			}

			LocalDateTime issuedAt = LocalDateTime.now(clock);
			CouponIssue couponIssue = new CouponIssue();
			couponIssue.setCouponId(request.couponId());
			couponIssue.setUserId(resolvedUserId);
			couponIssue.setIssuedAt(issuedAt);

			try {
				couponRepository.insertCouponIssue(couponIssue);
			} catch (DataIntegrityViolationException ex) {
				outcome = "duplicate";
				throw new ConflictException("이미 발급된 쿠폰입니다.", ex);
			}

			CouponStock latestStock = couponRepository.findCouponStock(request.couponId());
			outcome = "success";
			if (latestStock != null) {
				trafficMetricsService.updateCouponRemaining(request.couponId(), latestStock.getRemainCount());
			}

			return new CouponIssueResponse(
				request.couponId(),
				resolvedUserId,
				"ISSUED",
				latestStock == null ? null : latestStock.getRemainCount(),
				issuedAt);
		} finally {
			long durationMs = Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
			trafficMetricsService.recordCouponIssue(request.couponId(), outcome, durationMs);
		}
	}

	public CouponStock getCouponStock(Long couponId) {
		return couponRepository.findCouponStock(couponId);
	}

	public List<CouponSummaryResponse> findCouponStocks(String userId) {
		Map<Long, CouponIssue> issuedCouponMap = (userId == null || userId.isBlank()
			? List.<CouponIssue>of()
			: couponRepository.findCouponIssuesByUserId(userId.trim()))
			.stream()
			.collect(Collectors.toMap(CouponIssue::getCouponId, Function.identity(), (left, right) -> left));

		return couponRepository.findCouponStocks()
			.stream()
			.map(couponStock -> {
				CouponIssue issuedCoupon = issuedCouponMap.get(couponStock.getCouponId());
				return new CouponSummaryResponse(
					couponStock.getCouponId(),
					couponStock.getCouponName(),
					couponStock.getTotalCount(),
					couponStock.getRemainCount(),
					couponStock.getStatus(),
					"ACTIVE".equalsIgnoreCase(couponStock.getStatus())
						&& couponStock.getRemainCount() != null
						&& couponStock.getRemainCount() > 0,
					issuedCoupon != null,
					issuedCoupon == null ? null : issuedCoupon.getIssuedAt());
			})
			.toList();
	}

	@Transactional
	public CouponResetResponse resetIssuedCoupons(String userId) {
		String resolvedUserId = userId == null ? "" : userId.trim();
		List<CouponIssue> issuedCoupons = couponRepository.findCouponIssuesByUserId(resolvedUserId);
		int resetCount = 0;

		for (CouponIssue issuedCoupon : issuedCoupons) {
			int deletedCount = couponRepository.deleteCouponIssueByCouponIdAndUserId(issuedCoupon.getCouponId(), resolvedUserId);
			if (deletedCount == 1) {
				couponRepository.increaseStock(issuedCoupon.getCouponId());
				CouponStock updatedStock = couponRepository.findCouponStock(issuedCoupon.getCouponId());
				if (updatedStock != null) {
					trafficMetricsService.updateCouponRemaining(issuedCoupon.getCouponId(), updatedStock.getRemainCount());
				}
				resetCount += 1;
			}
		}

		return new CouponResetResponse(
			resolvedUserId,
			resetCount,
			resetCount > 0
				? "발급받은 쿠폰 이력을 초기화했습니다."
				: "초기화할 쿠폰 발급 이력이 없습니다.");
	}
}
