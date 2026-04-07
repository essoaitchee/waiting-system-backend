package com.example.waitingsystem.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DemoTrafficStateService {

	private final StringRedisTemplate stringRedisTemplate;

	public DemoTrafficStateService(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void saveActiveCouponId(Long eventId, Long couponId) {
		if (eventId == null || couponId == null) {
			return;
		}

		stringRedisTemplate.opsForValue().set(activeCouponKey(eventId), String.valueOf(couponId));
	}

	public Long getActiveCouponId(Long eventId, Long fallbackCouponId) {
		if (eventId == null) {
			return fallbackCouponId;
		}

		String value = stringRedisTemplate.opsForValue().get(activeCouponKey(eventId));
		if (value == null || value.isBlank()) {
			return fallbackCouponId;
		}

		try {
			return Long.parseLong(value);
		} catch (NumberFormatException ex) {
			return fallbackCouponId;
		}
	}

	private String activeCouponKey(Long eventId) {
		return "demo:event:coupon:" + eventId;
	}
}
