package com.rentitup.catalog_service.grpc.server;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.mapper.CatalogMapper;
import com.rentitup.catalog_service.service.CategoryService;
import com.rentitup.catalog_service.util.PaginationHelper;
import com.rentitup.shared.proto.catalog.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class CategoryGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {
	private final CategoryService categoryService;
	private final CatalogMapper catalogMapper;

	@Override
	public void createCategory(CreateCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
		try {
			CategoryEntity toCreate = catalogMapper.toEntity(request);
			CategoryEntity created = categoryService.createCategory(toCreate);

			Category responseCategory = catalogMapper.toProto(created);
			CategoryResponse response = CategoryResponse.newBuilder()
				.setResponse(responseCategory)
				.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to create category", ex);
			responseObserver.onError(
				Status.INTERNAL
					.withDescription("Failed to create category")
					.withCause(ex)
					.asRuntimeException()
			);
		}
	}

	@Override
	public void getCategory(GetCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
		try {
			UUID id = UUID.fromString(request.getId());
			CategoryEntity category = categoryService.getCategoryById(id);
			Category responseCategory = catalogMapper.toProto(category);
			CategoryResponse response =CategoryResponse.newBuilder()
					.setResponse(responseCategory)
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}catch (Exception ex) {
			log.error("Failed to get Category", ex);
			responseObserver.onError(
					Status.INTERNAL
							.withDescription("Failed to get category")
							.withCause(ex)
							.asRuntimeException()
			);
		}
	}

	@Override
	public void listCategories(ListCategoriesRequest request, StreamObserver<ListCategoriesResponse> responseObserver) {
		Pageable pageable = PaginationHelper.toPageable(request.getPagination());
		Page<CategoryEntity> page = categoryService.getCategories(pageable,request.getIncludeEmpty());
		ListCategoriesResponse.Builder responseBuilder = ListCategoriesResponse.newBuilder()
				.setPagination(PaginationHelper.toProto(page));

		page.getContent().forEach(categoryEntity ->
				responseBuilder.addCategories(catalogMapper.toProto(categoryEntity)));
		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void updateCategory(UpdateCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
		UUID id = UUID.fromString(request.getId());

		CategoryEntity updates = CategoryEntity.builder()
				.name(request.hasName() ? request.getName() : null)
				.description(request.hasDescription() ? request.getDescription() : null)
				.iconUrl(request.hasIconUrl() ? request.getIconUrl() : null)
				.defaultPriceType(request.hasDefaultPriceType()
						? catalogMapper.map(request.getDefaultPriceType())
						: null)
				.build();
		CategoryEntity updated = categoryService.updateCategory(id, updates);

		CategoryResponse response =CategoryResponse.newBuilder()
				.setResponse(catalogMapper.toProto(updated))
				.build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void deleteCategory(DeleteCategoryRequest request, StreamObserver<DeleteCategoryResponse> responseObserver) {
		UUID id = UUID.fromString(request.getId());
		categoryService.deleteCategoryById(id);

		DeleteCategoryResponse response = DeleteCategoryResponse.newBuilder()
				.setMessage("Category deleted")
				.build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
