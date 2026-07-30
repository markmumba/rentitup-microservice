package com.rentitup.catalog_service.service;

import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.common.exceptions.ForbiddenException;
import com.rentitup.common.exceptions.UnauthorizedException;
import com.rentitup.common.grpc.server.GrpcAuthContext;
import io.grpc.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogAuthorizationServiceTest {

	@Mock
	private MachineService machineService;

	private CatalogAuthorizationService authorizationService;

	@BeforeEach
	void setUp() {
		authorizationService = new CatalogAuthorizationService(machineService);
	}

	@Test
	void ownerCanManageOwnMachine() {
		UUID ownerId = UUID.randomUUID();
		UUID machineId = UUID.randomUUID();
		when(machineService.getMachine(machineId))
				.thenReturn(MachineEntity.builder().ownerId(ownerId).build());

		assertDoesNotThrow(() -> runAsUser(ownerId, "OWNER",
				() -> authorizationService.requireMachineOwnerOrAdmin(machineId)));
	}

	@Test
	void ownerCannotManageAnotherOwnersMachine() {
		UUID ownerId = UUID.randomUUID();
		UUID machineId = UUID.randomUUID();
		when(machineService.getMachine(machineId))
				.thenReturn(MachineEntity.builder().ownerId(UUID.randomUUID()).build());

		assertThrows(ForbiddenException.class, () -> runAsUser(ownerId, "OWNER",
				() -> authorizationService.requireMachineOwnerOrAdmin(machineId)));
	}

	@Test
	void adminCanManageAnyMachineWithoutOwnershipLookup() {
		assertDoesNotThrow(() -> runAsUser(UUID.randomUUID(), "ADMIN",
				() -> authorizationService.requireMachineOwnerOrAdmin(UUID.randomUUID())));
	}

	@Test
	void customerCannotManageMachine() {
		assertThrows(ForbiddenException.class, () -> runAsUser(UUID.randomUUID(), "CUSTOMER",
				() -> authorizationService.requireMachineOwnerOrAdmin(UUID.randomUUID())));
	}

	@Test
	void ownerCanOnlyCreateMachineForSelf() {
		UUID ownerId = UUID.randomUUID();

		assertDoesNotThrow(() -> runAsUser(ownerId, "OWNER",
				() -> authorizationService.requireOwnerCreation(ownerId)));
		assertThrows(ForbiddenException.class, () -> runAsUser(ownerId, "OWNER",
				() -> authorizationService.requireOwnerCreation(UUID.randomUUID())));
	}

	@Test
	void serviceCanAccessOwnerScopedInternalLookup() {
		Context context = Context.current()
				.withValue(GrpcAuthContext.IS_AUTHENTICATED, true)
				.withValue(GrpcAuthContext.TOKEN_TYPE, "SERVICE")
				.withValue(GrpcAuthContext.CLIENT_ID, "booking-service");

		assertDoesNotThrow(() -> context.run(
				() -> authorizationService.requireOwnerOrAdminOrService(UUID.randomUUID())));
	}

	@Test
	void unauthenticatedRequestIsRejected() {
		assertThrows(UnauthorizedException.class,
				() -> authorizationService.requireOwnerOrAdminOrService(UUID.randomUUID()));
	}

	private void runAsUser(UUID userId, String role, Runnable action) {
		Context.current()
				.withValue(GrpcAuthContext.IS_AUTHENTICATED, true)
				.withValue(GrpcAuthContext.TOKEN_TYPE, "USER")
				.withValue(GrpcAuthContext.USER_ID, userId.toString())
				.withValue(GrpcAuthContext.ROLE, role)
				.run(action);
	}
}
