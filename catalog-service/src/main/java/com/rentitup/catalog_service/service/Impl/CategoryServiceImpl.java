package com.rentitup.catalog_service.service.Impl;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.repository.CategoryRepository;
import com.rentitup.catalog_service.service.CategoryService;
import com.rentitup.shared_libs.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;

	@Override
	public CategoryEntity createCategory(CategoryEntity category) {
		if (categoryRepository.findByName(category.getName()).isPresent()) {
			throw new BadRequestException("Category already exists : " + category.getName());
		}

		CategoryEntity saved = categoryRepository.save(category);
		log.info("Created category id={} name={}", saved.getId(), saved.getName());
		return saved;
	}

	@Override
	public CategoryEntity getCategoryById(UUID id) {
		log.info("Retrieving category id={}", id);
		return categoryRepository.findById(id).orElseThrow(
				() -> new BadRequestException("Category not found with id=" + id)
		);

	}

	@Override
	public Page<CategoryEntity> getCategories(Pageable pageable, boolean includeEmpty) {
		log.info("Retrieving categories including empty={}", includeEmpty);
		if (includeEmpty) {
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
				throw new BadRequestException("Category already exists : " + updates.getName());
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
		return  categoryRepository.save(existing);
	}

	@Override
	public void deleteCategoryById(UUID id) {
		log.info("Deleting category id={}", id);
		CategoryEntity saved = categoryRepository.findById(id).orElseThrow(
				() -> new BadRequestException("Category not found with id=" + id)
		);
		categoryRepository.delete(saved);
	}
}
