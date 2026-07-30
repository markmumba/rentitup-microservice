package com.rentitup.catalog_service.service;

import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.common.exceptions.ForbiddenException;
import com.rentitup.common.exceptions.UnauthorizedException;
import com.rentitup.common.grpc.server.GrpcAuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogAuthorizationService {

	private final MachineService machineService;
	private static final String OWNER = "OWNER";

	public void requireOwnerCreation(UUID ownerId) {
		requireUser();
		if (!OWNER.equals(GrpcAuthContext.getRole())) {
			throw new ForbiddenException("Only an owner can create a machine");
		}
		if (!ownerId.toString().equals(GrpcAuthContext.getUserId())) {
			throw new ForbiddenException("A machine can only be created for the authenticated owner");
		}
	}

	public void requireMachineOwnerOrAdmin(UUID machineId) {
		requireUser();
		if (isAdmin()) {
			return;
		}
		if (!OWNER.equals(GrpcAuthContext.getRole())) {
			throw new ForbiddenException("Only an owner or admin can manage a machine");
		}

		MachineEntity machine = machineService.getMachine(machineId);
		if (!machine.getOwnerId().toString().equals(GrpcAuthContext.getUserId())) {
			throw new ForbiddenException("The authenticated user does not own this machine");
		}
	}

	public void requireOwnerOrAdminOrService(UUID ownerId) {
		requireAuthenticated();
		if (GrpcAuthContext.isServiceToken() || isAdmin()) {
			return;
		}
		if (!ownerId.toString().equals(GrpcAuthContext.getUserId())) {
			throw new ForbiddenException("The authenticated user cannot access this owner's resources");
		}
	}

	private void requireUser() {
		requireAuthenticated();
		if (!GrpcAuthContext.isUserToken()) {
			throw new ForbiddenException("This operation requires an authenticated user");
		}
	}

	private void requireAuthenticated() {
		if (!GrpcAuthContext.isAuthenticated()) {
			throw new UnauthorizedException("Authentication is required");
		}
	}

	private boolean isAdmin() {
		return "ADMIN".equals(GrpcAuthContext.getRole());
	}
}
