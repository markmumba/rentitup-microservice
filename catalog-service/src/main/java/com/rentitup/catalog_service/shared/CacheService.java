package com.rentitup.catalog_service.shared;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rentitup.catalog_service.entities.MachineImageEntity;
import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;


@Service
@Slf4j
public class CacheService {

	private final JdbcTemplate cacheJdbcTemplate;
	private final ObjectMapper objectMapper;

	public CacheService(@Qualifier("cacheJdbcTemplate") JdbcTemplate cacheJdbcTemplate, ObjectMapper objectMapper) {
		this.cacheJdbcTemplate = cacheJdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public Optional<CategoryEntity> getCategory(UUID key) {
		try {
			String result = cacheJdbcTemplate.queryForObject(
					"SELECT value FROM category_cache WHERE id = ?::uuid", String.class, key
			);
			return Optional.of(objectMapper.readValue(result, CategoryEntity.class));
		} catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		} catch (Exception ex) {
			throw new RuntimeException("failed to deserialize object", ex);
		}
	}


	public void putCategory(CategoryEntity category) {

		try {
			String json = objectMapper.writeValueAsString(category);
			cacheJdbcTemplate.update(
					"""
							INSERT INTO category_cache (id,value,inserted_at)
							VALUES (?::uuid ,?::jsonb, NOW())
							ON CONFLICT (id) DO UPDATE
							SET value = EXCLUDED.value, inserted_at = NOW()
							""",
					category.getId(), json
			);
		} catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}

	}

	public void evictCategory(UUID key) {
		cacheJdbcTemplate.update(
				"DELETE FROM category_cache WHERE id=?::uuid", key
		);
	}

	public Optional<MachineEntity> getMachineEntity(UUID key) {
		try {
			String json = cacheJdbcTemplate.queryForObject(
					"""
							SELECT value FROM machine_cache WHERE id=?::uuid
							""", String.class, key);
			return Optional.of(objectMapper.readValue(json, MachineEntity.class));
		} catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		} catch (Exception ex) {
			throw new RuntimeException("failed to deserialize object", ex);
		}
	}

	public void putMachine(MachineEntity machine) {
		try {
			String json = objectMapper.writeValueAsString(machine);
			cacheJdbcTemplate.update(
					"""
							INSERT INTO machine_cache (id,value,inserted_at)
							VALUES (?::uuid,?::jsonb,NOW())
							ON CONFLICT (id) DO UPDATE
							SET value = EXCLUDED.value, inserted_at = NOW()
							""",
					machine.getId(), json
			);
		} catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}
	}

	public void evictMachine(UUID key) {
		cacheJdbcTemplate.update("DELETE FROM machine_cache WHERE id=?::uuid", key);
	}

	public void putMachineImages(UUID machineId, MachineImageEntity machineImage) {
		try{
			String json = objectMapper.writeValueAsString(machineImage);
			cacheJdbcTemplate.update(
					"""
                     INSERT INTO machine_image_cache (id,value,inserted_at)
                     VALUES (?::uuid,?::jsonb,NOW())
                     ON CONFLICT (id) DO UPDATE
                     SET value = machine_image_cache.value ||  ?::jsonb , inserted_at = NOW()
                     """,
					machineId,
					"[" + json + "]",
					"[" + json + "]"
			);
		}catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}
	}

	public void putMachineImages(UUID machineId, List<MachineImageEntity> images) {
		try {
			String json = objectMapper.writeValueAsString(images);

			cacheJdbcTemplate.update("""
                     INSERT INTO machine_image_cache (id,value,inserted_at)
                     VALUES (?::uuid,?::jsonb,NOW())
                     ON CONFLICT (id) DO UPDATE
                     SET value = EXCLUDED.value, inserted_at = NOW()
                     """,
					machineId,json
			);
		}catch (Exception ex) {
			throw  new RuntimeException("failed to serialize object", ex);
		}
	}


	public Optional<List<MachineImageEntity>> getMachineImages(UUID  machineId) {
		try {
			String json = cacheJdbcTemplate.queryForObject(
					"SELECT value FROM machine_image_cache WHERE id=?::uuid", String.class, machineId
			);
			List<MachineImageEntity> images = objectMapper.readValue(json,
					objectMapper.getTypeFactory()
							.constructCollectionType(List.class, MachineImageEntity.class));
			return Optional.of(images);

		}catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}
	}
	public void evictMachineImages(UUID  machineId) {
		cacheJdbcTemplate.update("DELETE FROM machine_image_cache WHERE id=?::uuid", machineId);
	}

	public Optional<List<MaintenanceRecordEntity>> getMaintenanceRecord(UUID machineId) {
		try {
			String json = cacheJdbcTemplate.queryForObject(
					"SELECT value FROM maintenance_records_cache WHERE id=?::uuid", String.class, machineId
			);
			List<MaintenanceRecordEntity> records = objectMapper.readValue(
					json,
					objectMapper.getTypeFactory()
							.constructCollectionType(List.class, MaintenanceRecordEntity.class)
			);
			return Optional.of(records);
		}catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}
		catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}
	}

	public void putMaintenanceRecord(UUID machineId, MaintenanceRecordEntity maintenanceRecord) {
		try {
			String json = objectMapper.writeValueAsString(maintenanceRecord);
			cacheJdbcTemplate.update(
					"""
                      INSERT INTO machine_records_cache (id,value,inserted_at)
                      VALUES (?::uuid,?::jsonb,NOW())
                      ON CONFLICT (id) DO UPDATE
                      SET value = machine_records_cache.value || ?::jsonb, inserted_at = NOW()
                      """,
					machineId,
					"[" + json + "]",
					"[" + json + "]"
			);
		}catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}
	}
}
