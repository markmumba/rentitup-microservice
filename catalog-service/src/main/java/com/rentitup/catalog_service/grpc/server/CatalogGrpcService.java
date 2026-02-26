package com.rentitup.catalog_service.grpc.server;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;
import com.rentitup.catalog_service.mapper.CatalogMapper;
import com.rentitup.catalog_service.service.CategoryService;
import com.rentitup.catalog_service.service.MachineService;
import com.rentitup.catalog_service.service.StorageService;
import com.rentitup.catalog_service.specification.MachineSpecification;
import com.rentitup.common.util.PaginationHelper;
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
	private final StorageService storageService;

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
		try {
			UUID id = UUID.fromString(request.getId());
			MachineEntity machine = machineService.getMachine(id);
			MachineResponse response = MachineResponse.newBuilder()
					.setMachine(catalogMapper.toProto(machine))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			log.warn("Invalid machine ID format: {}", request.getId());
			responseObserver.onError(Status.INVALID_ARGUMENT
					.withDescription("Invalid machine ID format")
					.asRuntimeException());
		} catch (Exception e) {
			log.error("Failed to get machine: {}", request.getId(), e);
			responseObserver.onError(Status.NOT_FOUND
					.withDescription(e.getMessage())
					.asRuntimeException());
		}
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

			getAllMachines(responseObserver, pageable, spec);
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

			getAllMachines(responseObserver, pageable, spec);
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

			getAllMachines(responseObserver, pageable, spec);
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

			getAllMachines(responseObserver, pageable, spec);
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

			getAllMachines(responseObserver, pageable, spec);
		} catch (Exception ex) {
			log.error("Failed to get nearby machines", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to get nearby machines")
					.withCause(ex)
					.asRuntimeException());
		}
	}

	private void getAllMachines(StreamObserver<ListMachinesResponse> responseObserver, Pageable pageable, Specification<MachineEntity> spec) {
		Page<MachineEntity> page = machineService.findAll(spec, pageable);

		ListMachinesResponse.Builder responseBuilder = ListMachinesResponse.newBuilder()
				.setPagination(PaginationHelper.toProto(page));

		page.getContent().forEach(machine ->
				responseBuilder.addMachines(catalogMapper.toProto(machine)));

		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getUploadUrl(GetUploadUrlRequest request, StreamObserver<GetUploadUrlResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());

			// Verify machine exists
			machineService.getMachine(machineId);

			StorageService.UploadUrlResult result = storageService.generateUploadUrl(
				machineId,
				request.getFilename(),
				request.getContentType()
			);

			GetUploadUrlResponse response = GetUploadUrlResponse.newBuilder()
				.setUploadUrl(result.uploadUrl())
				.setObjectKey(result.objectKey())
				.setPublicUrl(result.publicUrl())
				.setExpiresInSeconds(result.expiresInSeconds())
				.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (IllegalArgumentException ex) {
			log.error("Invalid machine ID: {}", request.getMachineId(), ex);
			responseObserver.onError(Status.INVALID_ARGUMENT
				.withDescription("Invalid machine ID")
				.asRuntimeException());
		} catch (Exception ex) {
			log.error("Failed to generate upload URL", ex);
			responseObserver.onError(Status.INTERNAL
				.withDescription("Failed to generate upload URL")
				.withCause(ex)
				.asRuntimeException());
		}
	}

	@Override
	public void addMachineImage(AddMachineImageRequest request, StreamObserver<MachineResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			MachineEntity machine = machineService.addImage(
				machineId,
				request.getUrl(),
				request.getIsPrimary()
			);

			MachineResponse response = MachineResponse.newBuilder()
				.setMachine(catalogMapper.toProto(machine))
				.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to add machine image", ex);
			responseObserver.onError(Status.INTERNAL
				.withDescription("Failed to add machine image")
				.withCause(ex)
				.asRuntimeException());
		}
	}

	@Override
	public void removeMachineImage(RemoveMachineImageRequest request, StreamObserver<MachineResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			UUID imageId = UUID.fromString(request.getImageId());

			MachineEntity machine = machineService.removeImage(machineId, imageId);

			MachineResponse response = MachineResponse.newBuilder()
				.setMachine(catalogMapper.toProto(machine))
				.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (IllegalArgumentException ex) {
			log.error("Invalid ID format", ex);
			responseObserver.onError(Status.INVALID_ARGUMENT
				.withDescription("Invalid machine ID or image ID format")
				.asRuntimeException());
		} catch (Exception ex) {
			log.error("Failed to remove machine image", ex);
			responseObserver.onError(Status.INTERNAL
				.withDescription("Failed to remove machine image")
				.withCause(ex)
				.asRuntimeException());
		}
	}

	@Override
	public void setPrimaryImage(SetPrimaryImageRequest request, StreamObserver<MachineResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			UUID imageId = UUID.fromString(request.getImageId());

			MachineEntity machine = machineService.setPrimaryImage(machineId, imageId);

			MachineResponse response = MachineResponse.newBuilder()
				.setMachine(catalogMapper.toProto(machine))
				.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (IllegalArgumentException ex) {
			log.error("Invalid ID format", ex);
			responseObserver.onError(Status.INVALID_ARGUMENT
				.withDescription("Invalid machine ID or image ID format")
				.asRuntimeException());
		} catch (Exception ex) {
			log.error("Failed to set primary image", ex);
			responseObserver.onError(Status.INTERNAL
				.withDescription("Failed to set primary image")
				.withCause(ex)
				.asRuntimeException());
		}
	}

	@Override
	public void addMaintenanceRecord(AddMaintenanceRecordRequest request, StreamObserver<MaintenanceRecordResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			MaintenanceRecordEntity maintenanceRecord = catalogMapper.toEntity(request);
			MaintenanceRecordEntity savedMaintenanceRecord = machineService.createMaintenanceRecord(maintenanceRecord, machineId);
			MaintenanceRecordResponse response = MaintenanceRecordResponse.newBuilder()
					.setRecord(catalogMapper.toProto(savedMaintenanceRecord))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (IllegalArgumentException ex) {
			log.error("Invalid machine ID: {}", request.getMachineId(), ex);
			responseObserver.onError(Status.INVALID_ARGUMENT
					.withDescription("Invalid machine ID")
					.asRuntimeException());
		} catch (Exception ex) {
			log.error("Failed to add maintenance record", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to add maintenance record")
					.withCause(ex)
					.asRuntimeException());
		}
	}

	@Override
	public void getMaintenanceHistory(GetMaintenanceHistoryRequest request, StreamObserver<MaintenanceHistoryResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());

			Page<MaintenanceRecordEntity> page = machineService.getMaintenanceHistory(machineId, pageable);

			MaintenanceHistoryResponse.Builder responseBuilder = MaintenanceHistoryResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent().forEach(record ->
					responseBuilder.addRecords(catalogMapper.toProto(record)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (IllegalArgumentException ex) {
			log.error("Invalid machine ID: {}", request.getMachineId(), ex);
			responseObserver.onError(Status.INVALID_ARGUMENT
					.withDescription("Invalid machine ID")
					.asRuntimeException());
		} catch (Exception ex) {
			log.error("Failed to get maintenance history", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to get maintenance history")
					.withCause(ex)
					.asRuntimeException());
		}
	}

	@Override
	public void getUpcomingMaintenance(GetUpcomingMaintenanceRequest request, StreamObserver<MaintenanceHistoryResponse> responseObserver) {
		try {
			UUID ownerId = UUID.fromString(request.getOwnerId());
			int daysAhead = request.getDaysAhead() > 0 ? request.getDaysAhead() : 30;

			// Default pagination since the request doesn't include it
			Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "nextServiceDate"));

			Page<MaintenanceRecordEntity> page = machineService.getUpcomingMaintenances(ownerId, daysAhead, pageable);

			MaintenanceHistoryResponse.Builder responseBuilder = MaintenanceHistoryResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent().forEach(record ->
					responseBuilder.addRecords(catalogMapper.toProto(record)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (IllegalArgumentException ex) {
			log.error("Invalid owner ID: {}", request.getOwnerId(), ex);
			responseObserver.onError(Status.INVALID_ARGUMENT
					.withDescription("Invalid owner ID")
					.asRuntimeException());
		} catch (Exception ex) {
			log.error("Failed to get upcoming maintenance", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to get upcoming maintenance")
					.withCause(ex)
					.asRuntimeException());
		}
	}

}
