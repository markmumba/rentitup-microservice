package com.rentitup.booking_service.grpc.client;


import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.catalog.CatalogServiceGrpc;
import com.rentitup.shared.proto.catalog.GetMachineRequest;
import com.rentitup.shared.proto.catalog.Machine;
import com.rentitup.shared.proto.catalog.MachineResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CatalogGrpcClient {

	@GrpcClient(value = "CATALOG-SERVICE", forwardToken = true)
	private CatalogServiceGrpc.CatalogServiceBlockingStub client;

	public Machine getMachine(String machineId) {
		GetMachineRequest request = GetMachineRequest.newBuilder()
				.setId(machineId)
				.build();

		MachineResponse machine = client.getMachine(request);
		return machine.getMachine();
	}

}
