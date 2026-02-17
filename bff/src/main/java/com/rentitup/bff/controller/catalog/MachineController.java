package com.rentitup.bff.controller.catalog;

import com.rentitup.bff.common.pagination.PaginationDto;
import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.bff.grpc.GrpcStubFactory;
import com.rentitup.shared.proto.catalog.*;
import com.rentitup.shared.proto.common.Location;
import com.rentitup.shared.proto.common.Money;
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
@RequestMapping("/api/v1/machines")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Machines", description = "Machine management endpoints")
public class MachineController {
	private final GrpcStubFactory grpcStubFactory;

	// ==================== Machine CRUD ====================

	@Operation(summary = "Create a new machine", description = "Creates a new machine listing")
	@PostMapping
	public ResponseEntity<?> createMachine(@Valid @RequestBody CreateMachineRequest request) {
		log.info("REST: Create machine request: {}", request.getName());
		MachineResponse response = grpcStubFactory.getCatalogStub().createMachine(request);
		return ResponseBuilder.created("Machine created successfully", response.getMachine());
	}

	@Operation(summary = "Get machine by ID", description = "Retrieves a single machine with optional owner and reviews")
	@GetMapping("/{id}")
	public ResponseEntity<?> getMachine(
			@Parameter(description = "Machine ID") @PathVariable String id,
			@Parameter(description = "Include owner details") @RequestParam(defaultValue = "false") boolean includeOwner,
			@Parameter(description = "Include reviews") @RequestParam(defaultValue = "false") boolean includeReviews) {
		log.info("REST: Get machine request: {}", id);
		GetMachineRequest request = GetMachineRequest.newBuilder()
				.setId(id)
				.setIncludeOwner(includeOwner)
				.setIncludeReviews(includeReviews)
				.build();
		MachineResponse response = grpcStubFactory.getCatalogStub().getMachine(request);
		return ResponseBuilder.success("Machine retrieved successfully", response.getMachine());
	}

	@Operation(summary = "Update a machine", description = "Updates an existing machine")
	@PutMapping("/{id}")
	public ResponseEntity<?> updateMachine(
			@Parameter(description = "Machine ID") @PathVariable String id,
			@Valid @RequestBody UpdateMachineRequest request) {
		log.info("REST: Update machine request: {}", id);

		UpdateMachineRequest.Builder builder = UpdateMachineRequest.newBuilder()
				.setId(id);

		if (request.hasName()) builder.setName(request.getName());
		if (request.hasDescription()) builder.setDescription(request.getDescription());
		if (request.hasBasePrice()) builder.setBasePrice(request.getBasePrice());
		if (request.hasPriceType()) builder.setPriceType(request.getPriceType());
		if (request.hasCondition()) builder.setCondition(request.getCondition());
		if (request.hasLocation()) builder.setLocation(request.getLocation());
		if (request.hasCategoryId()) builder.setCategoryId(request.getCategoryId());
		if (!request.getSpecificationsMap().isEmpty()) {
			builder.putAllSpecifications(request.getSpecificationsMap());
		}
		if (request.hasIsAvailable()) builder.setIsAvailable(request.getIsAvailable());

		MachineResponse response = grpcStubFactory.getCatalogStub().updateMachine(builder.build());
		return ResponseBuilder.success("Machine updated successfully", response.getMachine());
	}

	@Operation(summary = "Delete a machine", description = "Soft deletes a machine")
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteMachine(@Parameter(description = "Machine ID") @PathVariable String id) {
		log.info("REST: Delete machine request: {}", id);
		DeleteMachineRequest request = DeleteMachineRequest.newBuilder()
				.setId(id)
				.build();
		DeleteMachineResponse response = grpcStubFactory.getCatalogStub().deleteMachine(request);
		return ResponseBuilder.success(response.getMessage(), null);
	}

	// ==================== Machine Listing & Search ====================

	@Operation(summary = "List machines", description = "Retrieves a paginated list of machines with optional filters")
	@GetMapping
	public ResponseEntity<?> listMachines(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String categoryId,
			@RequestParam(required = false) MachineStatus status,
			@RequestParam(required = false) MachineCondition minCondition) {
		log.info("REST: List machines request - page: {}, size: {}", page, size);

		ListMachinesRequest.Builder builder = ListMachinesRequest.newBuilder()
				.setPagination(buildPaginationRequest(page, size));

		if (categoryId != null) builder.setCategoryId(categoryId);
		if (status != null) builder.setStatus(status);
		if (minCondition != null) builder.setMinCondition(minCondition);

		ListMachinesResponse response = grpcStubFactory.getCatalogStub().listMachines(builder.build());
		return buildMachineListResponse(response);
	}

	@Operation(summary = "Search machines", description = "Search machines with various filters including location, price, and condition")
	@GetMapping("/search")
	public ResponseEntity<?> searchMachines(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String query,
			@RequestParam(required = false) String categoryId,
			@RequestParam(required = false) String minPrice,
			@RequestParam(required = false) String maxPrice,
			@RequestParam(required = false) String currency,
			@RequestParam(required = false) MachineCondition minCondition,
			@RequestParam(required = false) Double minRating,
			@RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude,
			@RequestParam(required = false) Double radiusKm) {
		log.info("REST: Search machines request - query: {}", query);

		SearchMachinesRequest.Builder builder = SearchMachinesRequest.newBuilder()
				.setPagination(buildPaginationRequest(page, size));

		if (query != null) builder.setQuery(query);
		if (categoryId != null) builder.setCategoryId(categoryId);
		if (minPrice != null) {
			builder.setMinPrice(Money.newBuilder()
					.setAmount(minPrice)
					.setCurrency(currency != null ? currency : "USD")
					.build());
		}
		if (maxPrice != null) {
			builder.setMaxPrice(Money.newBuilder()
					.setAmount(maxPrice)
					.setCurrency(currency != null ? currency : "USD")
					.build());
		}
		if (minCondition != null) builder.setMinCondition(minCondition);
		if (minRating != null) builder.setMinRating(minRating);
		if (latitude != null && longitude != null) {
			builder.setNearLocation(Location.newBuilder()
					.setLatitude(latitude)
					.setLongitude(longitude)
					.build());
			if (radiusKm != null) builder.setRadiusKm(radiusKm);
		}

		ListMachinesResponse response = grpcStubFactory.getCatalogStub().searchMachines(builder.build());
		return buildMachineListResponse(response);
	}

	@Operation(summary = "Get machines by owner", description = "Retrieves all machines owned by a specific user")
	@GetMapping("/owner/{ownerId}")
	public ResponseEntity<?> getMachinesByOwner(
			@PathVariable String ownerId,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) MachineStatus status) {
		log.info("REST: Get machines by owner: {}", ownerId);

		GetMachinesByOwnerRequest.Builder builder = GetMachinesByOwnerRequest.newBuilder()
				.setOwnerId(ownerId)
				.setPagination(buildPaginationRequest(page, size));

		if (status != null) builder.setStatus(status);

		ListMachinesResponse response = grpcStubFactory.getCatalogStub().getMachinesByOwner(builder.build());
		return buildMachineListResponse(response);
	}

	@Operation(summary = "Get featured machines", description = "Retrieves top-rated available machines")
	@GetMapping("/featured")
	public ResponseEntity<?> getFeaturedMachines(
			@RequestParam(defaultValue = "10") int limit,
			@RequestParam(required = false) String categoryId) {
		log.info("REST: Get featured machines - limit: {}", limit);

		GetFeaturedMachinesRequest.Builder builder = GetFeaturedMachinesRequest.newBuilder()
				.setLimit(limit);

		if (categoryId != null) builder.setCategoryId(categoryId);

		ListMachinesResponse response = grpcStubFactory.getCatalogStub().getFeaturedMachines(builder.build());
		return ResponseBuilder.success("Featured machines retrieved", response.getMachinesList());
	}

	@Operation(summary = "Get nearby machines", description = "Retrieves machines within a specified radius of a location")
	@GetMapping("/nearby")
	public ResponseEntity<?> getNearbyMachines(
			@RequestParam double latitude,
			@RequestParam double longitude,
			@RequestParam(defaultValue = "10") double radiusKm,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String categoryId) {
		log.info("REST: Get nearby machines - lat: {}, lon: {}, radius: {}km", latitude, longitude, radiusKm);

		GetNearbyMachinesRequest.Builder builder = GetNearbyMachinesRequest.newBuilder()
				.setLocation(Location.newBuilder()
						.setLatitude(latitude)
						.setLongitude(longitude)
						.build())
				.setRadiusKm(radiusKm)
				.setPagination(buildPaginationRequest(page, size));

		if (categoryId != null) builder.setCategoryId(categoryId);

		ListMachinesResponse response = grpcStubFactory.getCatalogStub().getNearbyMachines(builder.build());
		return buildMachineListResponse(response);
	}

	// ==================== Machine Images ====================

	@Operation(summary = "Get upload URL", description = "Generates a pre-signed URL for uploading machine images")
	@PostMapping("/{machineId}/upload-url")
	public ResponseEntity<?> getUploadUrl(
			@PathVariable String machineId,
			@RequestParam String filename,
			@RequestParam String contentType) {
		log.info("REST: Get upload URL for machine: {}", machineId);

		GetUploadUrlRequest request = GetUploadUrlRequest.newBuilder()
				.setMachineId(machineId)
				.setFilename(filename)
				.setContentType(contentType)
				.build();

		GetUploadUrlResponse response = grpcStubFactory.getCatalogStub().getUploadUrl(request);
		return ResponseBuilder.success("Upload URL generated", response);
	}

	@Operation(summary = "Add machine image", description = "Adds an image to a machine after uploading")
	@PostMapping("/{machineId}/images")
	public ResponseEntity<?> addMachineImage(
			@PathVariable String machineId,
			@RequestBody AddMachineImageRequest request) {
		log.info("REST: Add image to machine: {}", machineId);

		AddMachineImageRequest grpcRequest = AddMachineImageRequest.newBuilder()
				.setMachineId(machineId)
				.setUrl(request.getUrl())
				.setIsPrimary(request.getIsPrimary())
				.build();

		MachineResponse response = grpcStubFactory.getCatalogStub().addMachineImage(grpcRequest);
		return ResponseBuilder.created("Image added successfully", response.getMachine());
	}

	@Operation(summary = "Remove machine image", description = "Removes an image from a machine")
	@DeleteMapping("/{machineId}/images/{imageId}")
	public ResponseEntity<?> removeMachineImage(
			@PathVariable String machineId,
			@PathVariable String imageId) {
		log.info("REST: Remove image {} from machine: {}", imageId, machineId);

		RemoveMachineImageRequest request = RemoveMachineImageRequest.newBuilder()
				.setMachineId(machineId)
				.setImageId(imageId)
				.build();

		MachineResponse response = grpcStubFactory.getCatalogStub().removeMachineImage(request);
		return ResponseBuilder.success("Image removed successfully", response.getMachine());
	}

	@Operation(summary = "Set primary image", description = "Sets an image as the primary image for a machine")
	@PutMapping("/{machineId}/images/{imageId}/primary")
	public ResponseEntity<?> setPrimaryImage(
			@PathVariable String machineId,
			@PathVariable String imageId) {
		log.info("REST: Set primary image {} for machine: {}", imageId, machineId);

		SetPrimaryImageRequest request = SetPrimaryImageRequest.newBuilder()
				.setMachineId(machineId)
				.setImageId(imageId)
				.build();

		MachineResponse response = grpcStubFactory.getCatalogStub().setPrimaryImage(request);
		return ResponseBuilder.success("Primary image set successfully", response.getMachine());
	}

	// ==================== Maintenance Records ====================

	@Operation(summary = "Add maintenance record", description = "Records a maintenance event for a machine")
	@PostMapping("/{machineId}/maintenance")
	public ResponseEntity<?> addMaintenanceRecord(
			@PathVariable String machineId,
			@RequestBody AddMaintenanceRecordRequest request) {
		log.info("REST: Add maintenance record for machine: {}", machineId);

		AddMaintenanceRecordRequest.Builder builder = AddMaintenanceRecordRequest.newBuilder()
				.setMachineId(machineId)
				.setServiceDate(request.getServiceDate())
				.setDescription(request.getDescription())
				.setPerformedBy(request.getPerformedBy());

		if (request.hasNextServiceDate()) {
			builder.setNextServiceDate(request.getNextServiceDate());
		}

		MaintenanceRecordResponse response = grpcStubFactory.getCatalogStub().addMaintenanceRecord(builder.build());
		return ResponseBuilder.created("Maintenance record added", response.getRecord());
	}

	@Operation(summary = "Get maintenance history", description = "Retrieves maintenance history for a machine")
	@GetMapping("/{machineId}/maintenance")
	public ResponseEntity<?> getMaintenanceHistory(
			@PathVariable String machineId,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		log.info("REST: Get maintenance history for machine: {}", machineId);

		GetMaintenanceHistoryRequest request = GetMaintenanceHistoryRequest.newBuilder()
				.setMachineId(machineId)
				.setPagination(buildPaginationRequest(page, size))
				.build();

		MaintenanceHistoryResponse response = grpcStubFactory.getCatalogStub().getMaintenanceHistory(request);

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		return ResponseBuilder.successPageResponse(
				response.getRecordsList(),
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}

	@Operation(summary = "Get upcoming maintenance", description = "Retrieves upcoming maintenance for all machines owned by a user")
	@GetMapping("/owner/{ownerId}/upcoming-maintenance")
	public ResponseEntity<?> getUpcomingMaintenance(
			@PathVariable String ownerId,
			@RequestParam(defaultValue = "30") int daysAhead) {
		log.info("REST: Get upcoming maintenance for owner: {}, daysAhead: {}", ownerId, daysAhead);

		GetUpcomingMaintenanceRequest request = GetUpcomingMaintenanceRequest.newBuilder()
				.setOwnerId(ownerId)
				.setDaysAhead(daysAhead)
				.build();

		MaintenanceHistoryResponse response = grpcStubFactory.getCatalogStub().getUpcomingMaintenance(request);

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		return ResponseBuilder.successPageResponse(
				response.getRecordsList(),
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}

	// ==================== Helper Methods ====================

	private PaginationRequest buildPaginationRequest(int page, int size) {
		return PaginationRequest.newBuilder()
				.setPage(page)
				.setSize(size)
				.build();
	}

	private ResponseEntity<?> buildMachineListResponse(ListMachinesResponse response) {
		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		List<Machine> machines = response.getMachinesList();
		return ResponseBuilder.successPageResponse(
				machines,
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}
}
