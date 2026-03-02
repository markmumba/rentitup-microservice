package com.rentitup.booking_service.grpc.client;


import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.catalog.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CatalogGrpcClient {

	@GrpcClient("CATALOG-SERVICE")
	private CatalogServiceGrpc.CatalogServiceBlockingStub client;

	public Machine getMachine(String machineId) {
		GetMachineRequest request = GetMachineRequest.newBuilder()
				.setId(machineId)
				.build();

		MachineResponse machine = client.getMachine(request);
		return machine.getMachine();
	}


	public List<UUID> getMachineIdsByOwnerId(String ownerId) {
		GetMachineIdsRequest request = GetMachineIdsRequest.newBuilder()
				.setOwnerId(ownerId)
				.build();
		GetMachineIdsResponse response = client.getMachineIdsByOwner(request);
		return response.getMachineIdsList().stream()
				.map(UUID::fromString)
				.toList();
	}

	public Machine updateTotalBookings(String machineId) {

	 UpdateMachineBookingsRequest request=UpdateMachineBookingsRequest.newBuilder()
			 .setMachineId(machineId)
			 .build();
		return client.updateMachineBookings(request).getMachine();
	}
}