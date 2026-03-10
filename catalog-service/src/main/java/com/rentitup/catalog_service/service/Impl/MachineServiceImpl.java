package com.rentitup.catalog_service.service.Impl;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.entities.MachineImageEntity;
import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;
import com.rentitup.catalog_service.enums.MachineStatus;
import com.rentitup.catalog_service.grpc.client.UserGrpcClient;
import com.rentitup.catalog_service.repository.CategoryRepository;
import com.rentitup.catalog_service.repository.MachineRepository;
import com.rentitup.catalog_service.repository.MaintenanceRecordRepository;
import com.rentitup.catalog_service.service.MachineService;
import com.rentitup.common.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MachineServiceImpl implements MachineService {

	private final CategoryRepository categoryRepository;
	private final MachineRepository machineRepository;
	private final MaintenanceRecordRepository maintenanceRecordRepository;
	private final UserGrpcClient userGrpcClient;



	@Override
	@Transactional
	public MachineEntity createMachine(MachineEntity machine, UUID categoryId) {
		CategoryEntity category = categoryRepository.findById(categoryId).orElseThrow(
				() -> new NotFoundException("Category not found: " + categoryId)
		);

		if (!userGrpcClient.userExists(machine.getOwnerId())) {
			throw new NotFoundException("Owner not found: " + machine.getOwnerId());
		}

		machine.setCategory(category);
		return machineRepository.save(machine);
	}

	@Override
	@Transactional(readOnly = true)
	public MachineEntity getMachine(UUID id) {
		return machineRepository.findById(id).orElseThrow(
				() -> new NotFoundException("Machine not found: " + id)
		);
	}

	@Override
	@Transactional
	public MachineEntity updateMachine(UUID id, MachineEntity updates, UUID categoryId) {
		log.info("Updating machine: {}", id);

		MachineEntity existing = getMachine(id);

		if (updates.getName() != null) {
			existing.setName(updates.getName());
		}
		if (updates.getDescription() != null) {
			existing.setDescription(updates.getDescription());
		}
		if (updates.getBasePrice() != null) {
			existing.setBasePrice(updates.getBasePrice());
		}
		if (updates.getPriceType() != null) {
			existing.setPriceType(updates.getPriceType());
		}
		if (updates.getCondition() != null) {
			existing.setCondition(updates.getCondition());
		}
		if (updates.getLatitude() != null) {
			existing.setLatitude(updates.getLatitude());
		}
		if (updates.getLongitude() != null) {
			existing.setLongitude(updates.getLongitude());
		}
		if (updates.getAddress() != null) {
			existing.setAddress(updates.getAddress());
		}
		if (updates.getCity() != null) {
			existing.setCity(updates.getCity());
		}
		if (updates.getSpecifications() != null && !updates.getSpecifications().isEmpty()) {
			existing.setSpecifications(updates.getSpecifications());
		}
		if (categoryId != null) {
			CategoryEntity category = categoryRepository.findById(categoryId).orElseThrow(
					() -> new NotFoundException("Category not found: " + categoryId)
			);
			existing.setCategory(category);
		}

		return machineRepository.save(existing);
	}

	@Override
	@Transactional
	public String deleteMachine(UUID id) {
		MachineEntity machine = getMachine(id);
		machine.setDeleted(true);
		machineRepository.save(machine);
		return "Machine deleted successfully";
	}

	@Override
	@Transactional(readOnly = true)
	public Page<MachineEntity> findAll(Specification<MachineEntity> spec, Pageable pageable) {
		return machineRepository.findAll(spec, pageable);
	}

	@Override
	public Page<MachineEntity> findFeaturedMachines(Specification<MachineEntity> spec, Pageable pageable) {
		return machineRepository.findAll(spec, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MachineEntity> findAllByIds(List<UUID> ids) {
		return machineRepository.findAllById(ids);
	}

	@Override
	@Transactional
	public MachineEntity addImage(UUID machineId, String url, boolean isPrimary) {
		MachineEntity machine = getMachine(machineId);

		if (isPrimary) {
			machine.getImages().forEach(img -> img.setPrimary(false));
		}

		int nextOrder = machine.getImages().stream()
			.mapToInt(MachineImageEntity::getDisplayOrder)
			.max()
			.orElse(-1) + 1;

		MachineImageEntity image = MachineImageEntity.builder()
			.url(url)
			.primary(isPrimary)
			.displayOrder(nextOrder)
			.build();

		machine.addImage(image);
		return machineRepository.save(machine);
	}

	@Override
	@Transactional
	public MachineEntity removeImage(UUID machineId, UUID imageId) {
		MachineEntity machine = getMachine(machineId);

		MachineImageEntity imageToRemove = machine.getImages().stream()
			.filter(img -> img.getId().equals(imageId))
			.findFirst()
			.orElseThrow(() -> new NotFoundException("Image not found: " + imageId));

		boolean wasPrimary = imageToRemove.isPrimary();
		machine.removeImage(imageToRemove);

		if (wasPrimary && !machine.getImages().isEmpty()) {
			machine.getImages().get(0).setPrimary(true);
		}

		return machineRepository.save(machine);
	}

	@Override
	@Transactional
	public MachineEntity setPrimaryImage(UUID machineId, UUID imageId) {
		MachineEntity machine = getMachine(machineId);

		MachineImageEntity newPrimary = machine.getImages().stream()
			.filter(img -> img.getId().equals(imageId))
			.findFirst()
			.orElseThrow(() -> new NotFoundException("Image not found: " + imageId));

		machine.getImages().forEach(img -> img.setPrimary(false));
		newPrimary.setPrimary(true);

		return machineRepository.save(machine);
	}

	@Override
	public MaintenanceRecordEntity createMaintenanceRecord(MaintenanceRecordEntity maintenanceRecord,UUID machineId) {
		MachineEntity machine = machineRepository.findById(machineId).orElseThrow(
				() -> new NotFoundException("Machine not found: " + machineId)
		);
		maintenanceRecord.setMachine(machine);
		return maintenanceRecordRepository.save(maintenanceRecord);
	}

	@Override
	public Page<MaintenanceRecordEntity> getMaintenanceHistory(UUID machineId, Pageable pageable) {
		MachineEntity machine = machineRepository.findById(machineId).orElseThrow(
				() -> new NotFoundException("Machine not found: " + machineId)
		);
		return maintenanceRecordRepository.findAllByMachine(machine,pageable);
	}

	@Override
	public Page<MaintenanceRecordEntity> getUpcomingMaintenances(UUID ownerId, int daysAhead, Pageable pageable) {
		if (userGrpcClient.userExists(ownerId)) {
			throw new NotFoundException("Owner not found: " + ownerId);
		}
		LocalDate now = LocalDate.now();
		LocalDate cutoffDate = now.plusDays(daysAhead);
		return maintenanceRecordRepository.findUpcomingByOwnerId(ownerId, now, cutoffDate, pageable);
	}

	@Override
	public List<UUID> getMachineIdsByOwner(UUID ownerId) {
		return machineRepository.findAllByOwnerId(ownerId).stream()
				.map(MachineEntity::getId)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean checkAvailability(UUID machineId, LocalDate startDate, LocalDate endDate) {
		MachineEntity machine = getMachine(machineId);

		if (machine.isDeleted()) {
			return false;
		}

		if (machine.getStatus() == MachineStatus.MAINTENANCE || machine.getStatus() == MachineStatus.INACTIVE) {
			return false;
		}

		if (!machine.isAvailable()) {
			return false;
		}

		// Check for maintenance scheduled during the requested period
		boolean hasMaintenanceConflict = maintenanceRecordRepository
				.existsByMachineAndNextServiceDateBetween(machine, startDate, endDate);

		return !hasMaintenanceConflict;
	}

	@Override
	@Transactional
	public MachineEntity updateMachineStatus(UUID machineId, MachineStatus status) {
		MachineEntity machine = getMachine(machineId);
		machine.setStatus(status);

		if (status == MachineStatus.AVAILABLE) {
			machine.setAvailable(true);
		} else if (status == MachineStatus.RENTED || status == MachineStatus.MAINTENANCE || status == MachineStatus.INACTIVE) {
			machine.setAvailable(false);
		}

		return machineRepository.save(machine);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, MachineEntity> getMachinesBatch(List<UUID> machineIds) {
		return machineRepository.findAllById(machineIds).stream()
				.collect(Collectors.toMap(MachineEntity::getId, Function.identity()));
	}

	@Override
	@Transactional
	public MachineEntity updateMachineRating(UUID machineId, BigDecimal newAverageRating, int totalReviews) {
		MachineEntity machine = getMachine(machineId);
		machine.setAverageRating(newAverageRating);
		machine.setTotalReviews(totalReviews);
		return machineRepository.save(machine);
	}

	@Override
	@Transactional
	public MachineEntity incrementTotalRentals(UUID machineId) {
		MachineEntity machine = getMachine(machineId);
		machine.setTotalRentals(machine.getTotalRentals() + 1);
		return machineRepository.save(machine);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MaintenanceRecordEntity> getAllMaintenanceRecords() {
		return maintenanceRecordRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Stream<MaintenanceRecordEntity> streamRecordsNeedingReminder(LocalDate endDate, Instant reminderCutoff) {
		return maintenanceRecordRepository.streamRecordsNeedingReminder(endDate, reminderCutoff);
	}

	@Override
	@Transactional
	public int markMaintenanceRecordsAsReminded(List<UUID> recordIds) {
		if (recordIds == null || recordIds.isEmpty()) {
			return 0;
		}
		return maintenanceRecordRepository.markAsReminded(recordIds, Instant.now());
	}

}
