package com.example.waitingsystem.repository;

import java.util.List;

import com.example.waitingsystem.domain.Product;
import org.apache.ibatis.annotations.Param;

public interface ProductRepository {

	List<Product> findProducts(
		@Param("categoryId") Long categoryId,
		@Param("offset") int offset,
		@Param("size") int size
	);

	List<Long> findActiveCategoryIds();
}
