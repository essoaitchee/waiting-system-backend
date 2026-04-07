package com.example.waitingsystem.service;

import java.util.List;

import com.example.waitingsystem.dto.ProductResponse;
import com.example.waitingsystem.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Cacheable(cacheNames = "product-list-v2", key = "(#categoryId == null ? 'ALL' : #categoryId) + ':' + #page + ':' + #size")
	public List<ProductResponse> getProducts(Long categoryId, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.max(size, 1);
		int startRow = safePage * safeSize + 1;
		int endRow = startRow + safeSize - 1;

		return productRepository.findProducts(categoryId, startRow, endRow)
			.stream()
			.map(product -> new ProductResponse(
				product.getProductId(),
				product.getCategoryId(),
				product.getProductName(),
				product.getPrice(),
				product.getStockCount(),
				product.getStatus()))
			.toList();
	}

	public List<Long> getActiveCategoryIds() {
		return productRepository.findActiveCategoryIds();
	}
}
