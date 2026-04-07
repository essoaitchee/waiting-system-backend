package com.example.waitingsystem.dto;

public record ProductResponse(
	Long productId,
	Long categoryId,
	String productName,
	Integer price,
	Integer stockCount,
	String status
) {
}
