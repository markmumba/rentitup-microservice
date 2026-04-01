package com.rentitup.catalog_service.service;

import com.rentitup.catalog_service.entities.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryService {
	CategoryEntity createCategory(CategoryEntity category);
	CategoryEntity getCategoryById(UUID id);
	Page<CategoryEntity> getCategories(Pageable pageable,boolean includeEmpty);
	CategoryEntity updateCategory(UUID id,CategoryEntity category);
	void deleteCategoryById(UUID id);
}
