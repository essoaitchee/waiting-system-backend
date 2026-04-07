package com.example.waitingsystem.repository;

import com.example.waitingsystem.domain.CouponIssue;
import com.example.waitingsystem.domain.CouponStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponRepository {

	CouponStock findCouponStock(@Param("couponId") Long couponId);

	List<CouponStock> findCouponStocks();

	List<CouponIssue> findCouponIssuesByUserId(@Param("userId") String userId);

	CouponIssue findCouponIssueByCouponIdAndUserId(@Param("couponId") Long couponId, @Param("userId") String userId);

	int decreaseStockIfAvailable(@Param("couponId") Long couponId);

	int increaseStock(@Param("couponId") Long couponId);

	void insertCouponIssue(CouponIssue couponIssue);

	int deleteCouponIssueByCouponIdAndUserId(@Param("couponId") Long couponId, @Param("userId") String userId);
}
