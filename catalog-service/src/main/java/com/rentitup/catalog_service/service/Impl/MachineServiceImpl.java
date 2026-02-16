package com.rentitup.catalog_service.service.Impl;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.repository.CategoryRepository;
import com.rentitup.catalog_service.repository.MachineRepository;
import com.rentitup.catalog_service.service.MachineService;
import com.rentitup.shared_libs.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MachineServiceImpl implements MachineService {

	private final CategoryRepository categoryRepository;
	private final MachineRepository machineRepository;

	@Override
	@Transactional
	public MachineEntity createMachine(MachineEntity machine, UUID categoryId) {
		CategoryEntity category = categoryRepository.findById(categoryId).orElseThrow(
				() -> new BadRequestException("Category not found: " + categoryId)
		);
		machine.setCategory(category);
		return machineRepository.save(machine);
	}

	@Override
	@Transactional(readOnly = true)
	public MachineEntity getMachine(UUID id) {
		return machineRepository.findById(id).orElseThrow(
				() -> new BadRequestException("Machine not found: " + id)
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
					() -> new BadRequestException("Category not found: " + categoryId)
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
	@Transactional(readOnly = true)
	public List<MachineEntity> findAllByIds(List<UUID> ids) {
		return machineRepository.findAllById(ids);
	}
}
