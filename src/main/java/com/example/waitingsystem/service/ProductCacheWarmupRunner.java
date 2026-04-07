package com.example.waitingsystem.service;

import java.util.List;

import com.example.waitingsystem.config.CacheWarmUpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProductCacheWarmupRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ProductCacheWarmupRunner.class);

	private final ProductService productService;
	private final CacheWarmUpProperties cacheWarmUpProperties;

	public ProductCacheWarmupRunner(ProductService productService, CacheWarmUpProperties cacheWarmUpProperties) {
		this.productService = productService;
		this.cacheWarmUpProperties = cacheWarmUpProperties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!cacheWarmUpProperties.isEnabled()) {
			return;
		}

		int warmedCount = 0;
		List<Long> categoryIds = productService.getActiveCategoryIds();

		for (Long categoryId : categoryIds) {
			for (int page = 0; page < cacheWarmUpProperties.getMaxPagesPerCategory(); page++) {
				List<?> products = productService.getProducts(categoryId, page, cacheWarmUpProperties.getPageSize());
				if (products.isEmpty()) {
					break;
				}

				warmedCount += products.size();
				if (warmedCount >= cacheWarmUpProperties.getTargetCount()) {
					log.info("Product cache warm-up completed. warmedCount={}", warmedCount);
					return;
				}
			}
		}

		log.info("Product cache warm-up finished. warmedCount={}", warmedCount);
	}
}
