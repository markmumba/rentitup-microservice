package com.rentitup.catalog_service.service.Impl;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.repository.CategoryRepository;
import com.rentitup.catalog_service.service.CategoryService;
import com.rentitup.catalog_service.shared.CacheService;
import com.rentitup.common.exceptions.ConflictException;
import com.rentitup.common.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;
	private final CacheService cacheService;

	@Override
	public CategoryEntity createCategory(CategoryEntity category) {
		if (categoryRepository.findByName(category.getName()).isPresent()) {
			throw new ConflictException("Category already exists: " + category.getName());
		}

		CategoryEntity saved = categoryRepository.save(category);
		cacheService.putCategory(saved);
		log.info("Created category id={} name={}", saved.getId(), saved.getName());
		return saved;
	}

	@Override
	public CategoryEntity getCategoryById(UUID id) {
		log.info("Retrieving category id={}", id);
		Optional<CategoryEntity> cached= cacheService.getCategory(id);
		if(cached.isPresent()) {
		    log.info("Cache hit for category id={}", id);
			return cached.get();
		}
		log.info("Cache miss for category id={}",id);
		CategoryEntity category =categoryRepository.findById(id).orElseThrow(
				() -> new NotFoundException("Category not found: " + id)
		);
		cacheService.putCategory(category);
		return category;
	}

	@Override
	public Page<CategoryEntity> getCategories(Pageable pageable, boolean includeEmpty) {
		log.info("Retrieving categories including empty={}", includeEmpty);
		if (!includeEmpty) {
			return categoryRepository.findAllWithMachines(pageable);
		}
		return categoryRepository.findAll(pageable);

	}


	@Override
	public CategoryEntity updateCategory(UUID id,CategoryEntity updates) {
		log.info("Updating category id={}",id);
		CategoryEntity existing = getCategoryById(id);
		if (updates.getName() != null && !updates.getName().equals(existing.getName())) {
			if(categoryRepository.findByName(updates.getName()).isPresent()) {
				throw new ConflictException("Category already exists: " + updates.getName());
			}
			existing.setName(updates.getName());
		}
		if (updates.getDescription() != null) {
			existing.setDescription(updates.getDescription());
		}
		if (updates.getIconUrl() != null) {
			existing.setIconUrl(updates.getIconUrl());
		}
		if (updates.getDefaultPriceType() != null) {
			existing.setDefaultPriceType(updates.getDefaultPriceType());
		}
		CategoryEntity savedCategory = categoryRepository.save(existing);
		cacheService.putCategory(savedCategory);
		return savedCategory;
	}
	

	@Override
	public void deleteCategoryById(UUID id) {
		log.info("Deleting category id={}", id);
		CategoryEntity saved = categoryRepository.findById(id).orElseThrow(
				() -> new NotFoundException("Category not found: " + id)
		);
		categoryRepository.delete(saved);
		cacheService.evictCategory(id);
	}
}
