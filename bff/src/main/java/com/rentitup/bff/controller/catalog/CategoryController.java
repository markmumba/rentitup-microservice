package com.rentitup.bff.controller.catalog;

import com.rentitup.bff.common.pagination.PaginationDto;
import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.bff.grpc.GrpcClientFactory;
import com.rentitup.shared.proto.catalog.*;
import com.rentitup.shared.proto.common.PaginationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categories", description = "Category management endpoints")
public class CategoryController {
	private final GrpcClientFactory grpcClient;

	@Operation(summary = "Create a new category", description = "Creates a new machine category")
	@PostMapping
	public ResponseEntity<?> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
		log.info("REST: Create category request: {}", request.getName());
		CategoryResponse response = grpcClient.getCatalogClient().createCategory(request);
		return ResponseBuilder.created("Category created successfully", response.getResponse());
	}

	@Operation(summary = "Get category by ID", description = "Retrieves a single category by its ID")
	@GetMapping("/{id}")
	public ResponseEntity<?> getCategory(@Parameter(description = "Category ID") @PathVariable String id) {
		log.info("REST: Get category request: {}", id);
		GetCategoryRequest request = GetCategoryRequest.newBuilder()
				.setId(id)
				.build();
		CategoryResponse response = grpcClient.getCatalogClient().getCategory(request);
		return ResponseBuilder.success("Category retrieved successfully", response.getResponse());
	}

	@Operation(summary = "List all categories", description = "Retrieves a paginated list of categories")
	@GetMapping
	public ResponseEntity<?> listCategories(
			@Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
			@Parameter(description = "Include categories with no machines") @RequestParam(defaultValue = "false") boolean includeEmpty) {
		log.info("REST: List categories request - page: {}, size: {}, includeEmpty: {}", page, size, includeEmpty);

		PaginationRequest paginationRequest = PaginationRequest.newBuilder()
				.setPage(page)
				.setSize(size)
				.build();

		ListCategoriesRequest request = ListCategoriesRequest.newBuilder()
				.setPagination(paginationRequest)
				.setIncludeEmpty(includeEmpty)
				.build();

		ListCategoriesResponse response = grpcClient.getCatalogClient().listCategories(request);

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		List<Category> categories = response.getCategoriesList();
		return ResponseBuilder.successPageResponse(
				categories,
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}

	@Operation(summary = "Update a category", description = "Updates an existing category")
	@PutMapping("/{id}")
	public ResponseEntity<?> updateCategory(
			@Parameter(description = "Category ID") @PathVariable String id,
			@Valid @RequestBody UpdateCategoryRequest request) {
		log.info("REST: Update category request: {}", id);

		UpdateCategoryRequest.Builder builder = UpdateCategoryRequest.newBuilder()
				.setId(id);

		if (request.hasName()) {
			builder.setName(request.getName());
		}
		if (request.hasDescription()) {
			builder.setDescription(request.getDescription());
		}
		if (request.hasIconUrl()) {
			builder.setIconUrl(request.getIconUrl());
		}
		if (request.hasDefaultPriceType()) {
			builder.setDefaultPriceType(request.getDefaultPriceType());
		}

		CategoryResponse response = grpcClient.getCatalogClient().updateCategory(builder.build());
		return ResponseBuilder.success("Category updated successfully", response.getResponse());
	}

	@Operation(summary = "Delete a category", description = "Deletes a category by its ID")
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteCategory(@Parameter(description = "Category ID") @PathVariable String id) {
		log.info("REST: Delete category request: {}", id);
		DeleteCategoryRequest request = DeleteCategoryRequest.newBuilder()
				.setId(id)
				.build();
		DeleteCategoryResponse response = grpcClient.getCatalogClient().deleteCategory(request);
		return ResponseBuilder.success(response.getMessage(), null);
	}
}
