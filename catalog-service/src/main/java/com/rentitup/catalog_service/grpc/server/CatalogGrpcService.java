package com.rentitup.catalog_service.grpc.server;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.mapper.CatalogMapper;
import com.rentitup.catalog_service.service.CategoryService;
import com.rentitup.catalog_service.service.MachineService;
import com.rentitup.catalog_service.specification.MachineSpecification;
import com.rentitup.catalog_service.util.PaginationHelper;
import com.rentitup.shared.proto.catalog.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {
	private final CategoryService categoryService;
	private final MachineService machineService;
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

	// ==================== Machine Methods ====================

	@Override
	public void createMachine(CreateMachineRequest request, StreamObserver<MachineResponse> responseObserver) {
		UUID categoryId = UUID.fromString(request.getCategoryId());
		MachineEntity machine = catalogMapper.toEntity(request);
		MachineEntity createdMachine = machineService.createMachine(machine,categoryId);
		MachineResponse response = MachineResponse.newBuilder()
				.setMachine(catalogMapper.toProto(createdMachine))
				.build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void getMachine(GetMachineRequest request, StreamObserver<MachineResponse> responseObserver) {
		UUID id = UUID.fromString(request.getId());
		MachineEntity machine = machineService.getMachine(id);
		MachineResponse response = MachineResponse.newBuilder()
				.setMachine(catalogMapper.toProto(machine))
				.build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void updateMachine(UpdateMachineRequest request, StreamObserver<MachineResponse> responseObserver) {
		UUID id = UUID.fromString(request.getId());
		UUID categoryId = request.hasCategoryId() ? UUID.fromString(request.getCategoryId()) : null;
		MachineEntity updates = catalogMapper.toEntity(request);
		MachineEntity machine = machineService.updateMachine(id, updates, categoryId);
		MachineResponse response = MachineResponse.newBuilder()
				.setMachine(catalogMapper.toProto(machine))
				.build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void deleteMachine(DeleteMachineRequest request, StreamObserver<DeleteMachineResponse> responseObserver) {
		UUID id = UUID.fromString(request.getId());
		String message = machineService.deleteMachine(id);
		DeleteMachineResponse response = DeleteMachineResponse.newBuilder()
				.setMessage(message)
				.build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void listMachines(ListMachinesRequest request, StreamObserver<ListMachinesResponse> responseObserver) {
		try {
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());

			// Build specification from request filters
			Specification<MachineEntity> spec = Specification
					.where(MachineSpecification.notDeleted())
					.and(MachineSpecification.hasCategory(
							request.hasCategoryId() ? UUID.fromString(request.getCategoryId()) : null))
					.and(MachineSpecification.hasStatus(
							request.hasStatus() ? catalogMapper.map(request.getStatus()) : null))
					.and(MachineSpecification.hasMinCondition(
							request.hasMinCondition() ? catalogMapper.map(request.getMinCondition()) : null));

			Page<MachineEntity> page = machineService.findAll(spec, pageable);

			ListMachinesResponse.Builder responseBuilder = ListMachinesResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent().forEach(machine ->
					responseBuilder.addMachines(catalogMapper.toProto(machine)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to list machines", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to list machines")
					.withCause(ex)
					.asRuntimeException());
		}
	}

	@Override
	public void searchMachines(SearchMachinesRequest request, StreamObserver<ListMachinesResponse> responseObserver) {
		try {
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());

			// Build specification from search filters
			Specification<MachineEntity> spec = Specification
					.where(MachineSpecification.notDeleted())
					.and(MachineSpecification.isAvailable())
					.and(MachineSpecification.searchQuery(
							request.hasQuery() ? request.getQuery() : null))
					.and(MachineSpecification.hasCategory(
							request.hasCategoryId() ? UUID.fromString(request.getCategoryId()) : null))
					.and(MachineSpecification.priceRange(
							request.hasMinPrice() ? new BigDecimal(request.getMinPrice().getAmount()) : null,
							request.hasMaxPrice() ? new BigDecimal(request.getMaxPrice().getAmount()) : null))
					.and(MachineSpecification.hasMinCondition(
							request.hasMinCondition() ? catalogMapper.map(request.getMinCondition()) : null))
					.and(MachineSpecification.hasMinRating(
							request.hasMinRating() ? request.getMinRating() : null))
					.and(MachineSpecification.isNearby(
							request.hasNearLocation() ? BigDecimal.valueOf(request.getNearLocation().getLatitude()) : null,
							request.hasNearLocation() ? BigDecimal.valueOf(request.getNearLocation().getLongitude()) : null,
							request.hasRadiusKm() ? request.getRadiusKm() : null));

			Page<MachineEntity> page = machineService.findAll(spec, pageable);

			ListMachinesResponse.Builder responseBuilder = ListMachinesResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent().forEach(machine ->
					responseBuilder.addMachines(catalogMapper.toProto(machine)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to search machines", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to search machines")
					.withCause(ex)
					.asRuntimeException());
		}
	}

	@Override
	public void getMachinesByOwner(GetMachinesByOwnerRequest request, StreamObserver<ListMachinesResponse> responseObserver) {
		try {
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());

			Specification<MachineEntity> spec = Specification
					.where(MachineSpecification.notDeleted())
					.and(MachineSpecification.hasOwner(UUID.fromString(request.getOwnerId())))
					.and(MachineSpecification.hasStatus(
							request.hasStatus() ? catalogMapper.map(request.getStatus()) : null));

			Page<MachineEntity> page = machineService.findAll(spec, pageable);

			ListMachinesResponse.Builder responseBuilder = ListMachinesResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent().forEach(machine ->
					responseBuilder.addMachines(catalogMapper.toProto(machine)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to get machines by owner", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to get machines by owner")
					.withCause(ex)
					.asRuntimeException());
		}
	}

	@Override
	public void getFeaturedMachines(GetFeaturedMachinesRequest request, StreamObserver<ListMachinesResponse> responseObserver) {
		try {
			// Featured = available, high-rated, sorted by rating desc
			Pageable pageable = PageRequest.of(0, request.getLimit(), Sort.by(Sort.Direction.DESC, "averageRating"));

			Specification<MachineEntity> spec = Specification
					.where(MachineSpecification.notDeleted())
					.and(MachineSpecification.isAvailable())
					.and(MachineSpecification.hasCategory(
							request.hasCategoryId() ? UUID.fromString(request.getCategoryId()) : null));

			Page<MachineEntity> page = machineService.findAll(spec, pageable);

			ListMachinesResponse.Builder responseBuilder = ListMachinesResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent().forEach(machine ->
					responseBuilder.addMachines(catalogMapper.toProto(machine)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to get featured machines", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to get featured machines")
					.withCause(ex)
					.asRuntimeException());
		}
	}

	@Override
	public void getNearbyMachines(GetNearbyMachinesRequest request, StreamObserver<ListMachinesResponse> responseObserver) {
		try {
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());

			Specification<MachineEntity> spec = Specification
					.where(MachineSpecification.notDeleted())
					.and(MachineSpecification.isAvailable())
					.and(MachineSpecification.isNearby(
							BigDecimal.valueOf(request.getLocation().getLatitude()),
							BigDecimal.valueOf(request.getLocation().getLongitude()),
							request.getRadiusKm()))
					.and(MachineSpecification.hasCategory(
							request.hasCategoryId() ? UUID.fromString(request.getCategoryId()) : null));

			Page<MachineEntity> page = machineService.findAll(spec, pageable);

			ListMachinesResponse.Builder responseBuilder = ListMachinesResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent().forEach(machine ->
					responseBuilder.addMachines(catalogMapper.toProto(machine)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to get nearby machines", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to get nearby machines")
					.withCause(ex)
					.asRuntimeException());
		}
	}
}
