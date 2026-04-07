package com.example.waitingsystem.controller;

import com.example.waitingsystem.dto.CouponIssueRequest;
import com.example.waitingsystem.dto.CouponIssueResponse;
import com.example.waitingsystem.dto.CouponResetRequest;
import com.example.waitingsystem.dto.CouponResetResponse;
import com.example.waitingsystem.dto.CouponSummaryResponse;
import com.example.waitingsystem.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

	private final CouponService couponService;

	public CouponController(CouponService couponService) {
		this.couponService = couponService;
	}

	@GetMapping
	public List<CouponSummaryResponse> getCoupons(@RequestParam(required = false) String userId) {
		return couponService.findCouponStocks(userId);
	}

	@PostMapping("/issue")
	public CouponIssueResponse issueCoupon(@Valid @RequestBody CouponIssueRequest request) {
		return couponService.issueCoupon(request);
	}

	@PostMapping("/reset")
	public CouponResetResponse resetCoupons(@Valid @RequestBody CouponResetRequest request) {
		return couponService.resetIssuedCoupons(request.userId());
	}
}
