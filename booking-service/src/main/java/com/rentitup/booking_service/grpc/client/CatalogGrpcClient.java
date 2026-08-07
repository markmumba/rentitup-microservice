package com.rentitup.booking_service.grpc.client;


import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.common.exceptions.BadRequestException;
import com.rentitup.common.exceptions.ForbiddenException;
import com.rentitup.common.exceptions.NotFoundException;
import com.rentitup.common.exceptions.ServiceUnavailableException;
import com.rentitup.shared.proto.catalog.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class CatalogGrpcClient {

	@GrpcClient("CATALOG-SERVICE")
	private CatalogServiceGrpc.CatalogServiceBlockingStub client;

	public Machine getMachine(String machineId) {
		try {
			GetMachineRequest request = GetMachineRequest.newBuilder()
					.setId(machineId)
					.build();
			return client.getMachine(request).getMachine();
		} catch (StatusRuntimeException exception) {
			throw translate(exception, "machine", machineId);
		}
	}


	public List<UUID> getMachineIdsByOwnerId(String ownerId) {
		try {
			GetMachineIdsRequest request = GetMachineIdsRequest.newBuilder()
					.setOwnerId(ownerId)
					.build();
			GetMachineIdsResponse response = client.getMachineIdsByOwner(request);
			return response.getMachineIdsList().stream()
					.map(UUID::fromString)
					.toList();
		} catch (StatusRuntimeException exception) {
			throw translate(exception, "owner", ownerId);
		}
	}

	public Machine updateTotalBookings(String machineId) {
		try {
			UpdateMachineBookingsRequest request = UpdateMachineBookingsRequest.newBuilder()
					.setMachineId(machineId)
					.build();
			return client.updateMachineBookings(request).getMachine();
		} catch (StatusRuntimeException exception) {
			throw translate(exception, "machine", machineId);
		}
	}

	private RuntimeException translate(StatusRuntimeException exception, String resource, String id) {
		Status status = exception.getStatus();
		log.error("Catalog service call failed for {} {}: {}", resource, id, status);
		return switch (status.getCode()) {
			case NOT_FOUND -> new NotFoundException("Catalog " + resource + " not found: " + id);
			case INVALID_ARGUMENT -> new BadRequestException("Invalid catalog " + resource + " id: " + id);
			case PERMISSION_DENIED, UNAUTHENTICATED ->
					new ForbiddenException("Catalog service rejected access to " + resource + ": " + id);
			default -> new ServiceUnavailableException("Catalog service unavailable", exception);
		};
	}
}
