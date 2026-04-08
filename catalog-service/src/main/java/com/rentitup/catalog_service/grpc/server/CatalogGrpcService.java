package com.rentitup.catalog_service.grpc.server;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;
import com.rentitup.catalog_service.mapper.CatalogMapper;
import com.rentitup.catalog_service.service.CategoryService;
import com.rentitup.catalog_service.service.MachineService;
import com.rentitup.catalog_service.service.StorageService;
import com.rentitup.catalog_service.specification.MachineSpecification;
import com.rentitup.common.grpc.GrpcExceptionHandler;
import com.rentitup.common.util.PaginationHelper;
import com.rentitup.shared.proto.catalog.*;
import com.rentitup.shared.proto.common.Empty;
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

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CatalogGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {
	private final CategoryService categoryService;
	private final MachineService machineService;
	private final CatalogMapper catalogMapper;
	private final StorageService storageService;

	@Override
	@Transactional
	public void createCategory(CreateCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
		try {
			CategoryEntity toCreate = catalogMapper.toEntity(request);
			CategoryEntity created = categoryService.createCategory(toCreate);
			CategoryResponse response = CategoryResponse.newBuilder()
					.setResponse(catalogMapper.toProto(created))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Create category");
		}
	}

	@Override
	public void getCategory(GetCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
		try {
			UUID id = UUID.fromString(request.getId());
			CategoryEntity category = categoryService.getCategoryById(id);
			CategoryResponse response = CategoryResponse.newBuilder()
					.setResponse(catalogMapper.toProto(category))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get category");
		}
	}

	@Override
	public void listCategories(ListCategoriesRequest request, StreamObserver<ListCategoriesResponse> responseObserver) {
		try {
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());
			Page<CategoryEntity> page = categoryService.getCategories(pageable, request.getIncludeEmpty());
			ListCategoriesResponse.Builder responseBuilder = ListCategoriesResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));
			page.getContent().forEach(entity -> responseBuilder.addCategories(catalogMapper.toProto(entity)));
			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "List categories");
		}
	}

	@Override
	@Transactional
	public void updateCategory(UpdateCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
		try {
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
			CategoryResponse response = CategoryResponse.newBuilder()
					.setResponse(catalogMapper.toProto(updated))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Update category");
		}
	}

	@Override
	@Transactional
	public void deleteCategory(DeleteCategoryRequest request, StreamObserver<DeleteCategoryResponse> responseObserver) {
		try {
			UUID id = UUID.fromString(request.getId());
			categoryService.deleteCategoryById(id);
			DeleteCategoryResponse response = DeleteCategoryResponse.newBuilder()
					.setMessage("Category deleted")
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Delete category");
		}
	}

	// ==================== Machine Methods ====================

	@Override
	@Transactional
	public void createMachine(CreateMachineRequest request, StreamObserver<MachineResponse> responseObserver) {
		try {
			log.info("CreateMachine: ownerId='{}' categoryId='{}' name='{}'",
					request.getOwnerId(), request.getCategoryId(), request.getName());

			if (!request.hasCategoryId() || request.getCategoryId().isEmpty()) {
				responseObserver.onError(Status.INVALID_ARGUMENT
						.withDescription("category_id is required")
						.asRuntimeException());
				return;
			}
			if (request.getOwnerId().isEmpty()) {
				responseObserver.onError(Status.INVALID_ARGUMENT
						.withDescription("owner_id is required")
						.asRuntimeException());
				return;
			}
			UUID categoryId = UUID.fromString(request.getCategoryId());
			MachineEntity machine = catalogMapper.toEntity(request);
			MachineEntity createdMachine = machineService.createMachine(machine, categoryId);
			MachineResponse response = MachineResponse.newBuilder()
					.setMachine(catalogMapper.toProto(createdMachine))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Create machine");
		}
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
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get machine");
		}
	}

	@Override
	@Transactional
	public void updateMachine(UpdateMachineRequest request, StreamObserver<MachineResponse> responseObserver) {
		try {
			UUID id = UUID.fromString(request.getId());
			UUID categoryId = request.hasCategoryId() ? UUID.fromString(request.getCategoryId()) : null;
			MachineEntity updates = catalogMapper.toEntity(request);
			MachineEntity machine = machineService.updateMachine(id, updates, categoryId);
			MachineResponse response = MachineResponse.newBuilder()
					.setMachine(catalogMapper.toProto(machine))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Update machine");
		}
	}

	@Override
	@Transactional
	public void deleteMachine(DeleteMachineRequest request, StreamObserver<DeleteMachineResponse> responseObserver) {
		try {
			UUID id = UUID.fromString(request.getId());
			String message = machineService.deleteMachine(id);
			DeleteMachineResponse response = DeleteMachineResponse.newBuilder()
					.setMessage(message)
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Delete machine");
		}
	}

	@Override
	public void listMachines(ListMachinesRequest request, StreamObserver<ListMachinesResponse> responseObserver) {
		try {
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());
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
			GrpcExceptionHandler.handleException(ex, responseObserver, "List machines");
		}
	}

	@Override
	public void searchMachines(SearchMachinesRequest request, StreamObserver<ListMachinesResponse> responseObserver) {
		try {
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());
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
			GrpcExceptionHandler.handleException(ex, responseObserver, "Search machines");
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
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get machines by owner");
		}
	}

	@Override
	public void getFeaturedMachines(GetFeaturedMachinesRequest request, StreamObserver<ListMachinesResponse> responseObserver) {
		try {
			Pageable pageable = PageRequest.of(0, request.getLimit(), Sort.by(Sort.Direction.DESC, "averageRating"));
			Specification<MachineEntity> spec = Specification
					.where(MachineSpecification.notDeleted())
					.and(MachineSpecification.isAvailable());
			getAllMachines(responseObserver, pageable, spec);
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get featured machines");
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
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get nearby machines");
		}
	}

	@Override
	public void getMachineIdsByOwner(GetMachineIdsRequest request, StreamObserver<GetMachineIdsResponse> responseObserver) {
		try {
			UUID ownerId = UUID.fromString(request.getOwnerId());
			List<UUID> machineIds = machineService.getMachineIdsByOwner(ownerId);
			GetMachineIdsResponse response = GetMachineIdsResponse.newBuilder()
					.addAllMachineIds(machineIds.stream().map(UUID::toString).toList())
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get machine IDs by owner");
		}
	}

	private void getAllMachines(StreamObserver<ListMachinesResponse> responseObserver, Pageable pageable, Specification<MachineEntity> spec) {
		Page<MachineEntity> page = machineService.findAll(spec, pageable);
		ListMachinesResponse.Builder responseBuilder = ListMachinesResponse.newBuilder()
				.setPagination(PaginationHelper.toProto(page));
		page.getContent().forEach(machine -> responseBuilder.addMachines(catalogMapper.toProto(machine)));
		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getUploadUrl(GetUploadUrlRequest request, StreamObserver<GetUploadUrlResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			machineService.getMachine(machineId);
			StorageService.UploadUrlResult result = storageService.generateUploadUrl(
					machineId, request.getFilename(), request.getContentType());
			GetUploadUrlResponse response = GetUploadUrlResponse.newBuilder()
					.setUploadUrl(result.uploadUrl())
					.setObjectKey(result.objectKey())
					.setPublicUrl(result.publicUrl())
					.setExpiresInSeconds(result.expiresInSeconds())
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get upload URL");
		}
	}

	@Override
	@Transactional
	public void addMachineImage(AddMachineImageRequest request, StreamObserver<MachineResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			MachineEntity machine = machineService.addImage(machineId, request.getUrl(), request.getIsPrimary());
			MachineResponse response = MachineResponse.newBuilder()
					.setMachine(catalogMapper.toProto(machine))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Add machine image");
		}
	}

	@Override
	@Transactional
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
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Remove machine image");
		}
	}

	@Override
	@Transactional
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
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Set primary image");
		}
	}

	@Override
	@Transactional
	public void addMaintenanceRecord(AddMaintenanceRecordRequest request, StreamObserver<MaintenanceRecordResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			MaintenanceRecordEntity maintenanceRecord = catalogMapper.toEntity(request);
			MaintenanceRecordEntity saved = machineService.createMaintenanceRecord(maintenanceRecord, machineId);
			MaintenanceRecordResponse response = MaintenanceRecordResponse.newBuilder()
					.setRecord(catalogMapper.toProto(saved))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Add maintenance record");
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
			page.getContent().forEach(record -> responseBuilder.addRecords(catalogMapper.toProto(record)));
			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get maintenance history");
		}
	}

	@Override
	public void checkAvailability(CheckAvailabilityRequest request, StreamObserver<CheckAvailabilityResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			LocalDate startDate = LocalDate.of(
					request.getStartDate().getYear(),
					request.getStartDate().getMonth(),
					request.getStartDate().getDay());
			LocalDate endDate = LocalDate.of(
					request.getEndDate().getYear(),
					request.getEndDate().getMonth(),
					request.getEndDate().getDay());
			boolean isAvailable = machineService.checkAvailability(machineId, startDate, endDate);
			CheckAvailabilityResponse.Builder responseBuilder = CheckAvailabilityResponse.newBuilder()
					.setAvailable(isAvailable);
			if (!isAvailable) {
				responseBuilder.setReason("Machine is not available for the requested period");
			}
			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Check availability");
		}
	}

	@Override
	@Transactional
	public void updateMachineStatus(UpdateMachineStatusRequest request, StreamObserver<MachineResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			MachineEntity machine = machineService.updateMachineStatus(machineId, catalogMapper.map(request.getStatus()));
			MachineResponse response = MachineResponse.newBuilder()
					.setMachine(catalogMapper.toProto(machine))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Update machine status");
		}
	}

	@Override
	public void getMachinesBatch(GetMachineBatchRequest request, StreamObserver<GetMachinesBatchResponse> responseObserver) {
		try {
			List<UUID> machineIds = request.getMachineIdsList().stream()
					.map(UUID::fromString)
					.toList();
			Map<UUID, MachineEntity> machines = machineService.getMachinesBatch(machineIds);
			GetMachinesBatchResponse.Builder responseBuilder = GetMachinesBatchResponse.newBuilder();
			machines.forEach((id, entity) -> responseBuilder.putMachines(id.toString(), catalogMapper.toProto(entity)));
			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get machines batch");
		}
	}

	@Override
	@Transactional
	public void updateMachineRating(UpdateMachineRatingRequest request, StreamObserver<MachineResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			MachineEntity machine = machineService.updateMachineRating(
					machineId,
					BigDecimal.valueOf(request.getNewAverageRating()),
					request.getTotalReviews());
			MachineResponse response = MachineResponse.newBuilder()
					.setMachine(catalogMapper.toProto(machine))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Update machine rating");
		}
	}

	@Override
	@Transactional
	public void updateMachineBookings(UpdateMachineBookingsRequest request, StreamObserver<MachineResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			MachineEntity machine = machineService.incrementTotalRentals(machineId);
			MachineResponse response = MachineResponse.newBuilder()
					.setMachine(catalogMapper.toProto(machine))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Update machine bookings");
		}
	}

	@Override
	public void getMaintenanceRecords(Empty request, StreamObserver<ListMaintenanceRecordResponse> responseObserver) {
		try {
			List<MaintenanceRecordEntity> allRecords = machineService.getAllMaintenanceRecords();
			int batchSize = 100;
			for (int i = 0; i < allRecords.size(); i += batchSize) {
				int end = Math.min(i + batchSize, allRecords.size());
				List<MaintenanceRecordEntity> batch = allRecords.subList(i, end);
				ListMaintenanceRecordResponse.Builder responseBuilder = ListMaintenanceRecordResponse.newBuilder();
				batch.forEach(record -> responseBuilder.addRecords(catalogMapper.toProto(record)));
				responseObserver.onNext(responseBuilder.build());
			}
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get maintenance records");
		}
	}

	@Override
	public void getUpcomingMaintenance(GetUpcomingMaintenanceRequest request, StreamObserver<MaintenanceHistoryResponse> responseObserver) {
		try {
			UUID ownerId = UUID.fromString(request.getOwnerId());
			int daysAhead = request.getDaysAhead() > 0 ? request.getDaysAhead() : 30;
			Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "nextServiceDate"));
			Page<MaintenanceRecordEntity> page = machineService.getUpcomingMaintenances(ownerId, daysAhead, pageable);
			MaintenanceHistoryResponse.Builder responseBuilder = MaintenanceHistoryResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));
			page.getContent().forEach(record -> responseBuilder.addRecords(catalogMapper.toProto(record)));
			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get upcoming maintenance");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void getMaintenanceRecordsNeedingReminder(GetMaintenanceRecordsNeedingReminderRequest request,
			StreamObserver<ListMaintenanceRecordResponse> responseObserver) {
		try {
			int daysAhead = request.getDaysAhead() > 0 ? request.getDaysAhead() : 7;
			LocalDate endDate = LocalDate.now().plusDays(daysAhead);
			Instant reminderCutoff = Instant.now().minus(1, ChronoUnit.DAYS);

			final int BATCH_SIZE = 100;
			List<MaintenanceRecord> batch = new ArrayList<>();
			AtomicInteger count = new AtomicInteger(0);

			machineService.streamRecordsNeedingReminder(endDate, reminderCutoff).forEach(record -> {
				batch.add(catalogMapper.toProto(record));
				count.incrementAndGet();
				if (batch.size() >= BATCH_SIZE) {
					responseObserver.onNext(ListMaintenanceRecordResponse.newBuilder()
							.addAllRecords(batch).build());
					batch.clear();
				}
			});

			if (!batch.isEmpty()) {
				responseObserver.onNext(ListMaintenanceRecordResponse.newBuilder()
						.addAllRecords(batch).build());
			}

			log.info("Streamed {} maintenance records needing reminder", count.get());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Get maintenance records needing reminder");
		}
	}

	@Override
	@Transactional
	public void markMaintenanceReminded(MarkMaintenanceRemindedRequest request,
			StreamObserver<MarkMaintenanceRemindedResponse> responseObserver) {
		try {
			log.info("Marking {} maintenance records as reminded", request.getRecordIdsCount());
			List<UUID> recordIds = request.getRecordIdsList().stream()
					.map(UUID::fromString)
					.toList();
			int updatedCount = machineService.markMaintenanceRecordsAsReminded(recordIds);
			MarkMaintenanceRemindedResponse response = MarkMaintenanceRemindedResponse.newBuilder()
					.setUpdatedCount(updatedCount)
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			GrpcExceptionHandler.handleException(ex, responseObserver, "Mark maintenance reminded");
		}
	}
}
