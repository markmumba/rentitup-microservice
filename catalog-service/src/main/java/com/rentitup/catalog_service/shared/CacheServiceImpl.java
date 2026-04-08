package com.rentitup.catalog_service.shared;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
@Slf4j
public class CacheServiceImpl implements CacheService {

	private final JdbcTemplate cacheJdbcTemplate;
	private final ObjectMapper objectMapper;

	public CacheServiceImpl(@Qualifier("cacheJdbcTemplate") JdbcTemplate cacheJdbcTemplate, ObjectMapper objectMapper) {
		this.cacheJdbcTemplate = cacheJdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public Optional<CategoryEntity> getCategory(UUID key) {
		try {
			String result = cacheJdbcTemplate.queryForObject(
					"SELECT value FROM category_cache WHERE id = ?::uuid", String.class, key
			);
			log.debug("Category cache hit for {}", key);
			return Optional.of(objectMapper.readValue(result, CategoryEntity.class));
		} catch (EmptyResultDataAccessException ex) {
			log.debug("Category cache miss for {}", key);
			return Optional.empty();
		} catch (Exception ex) {
			throw new RuntimeException("failed to deserialize object", ex);
		}
	}


	public void putCategory(CategoryEntity category) {
		try {
			String json = objectMapper.writeValueAsString(category);
			int affectedRows = cacheJdbcTemplate.update(
					"""
							INSERT INTO category_cache (id,value,inserted_at)
							VALUES (?::uuid ,?::jsonb, NOW())
							ON CONFLICT (id) DO UPDATE
							SET value = EXCLUDED.value, inserted_at = NOW()
							""",
					category.getId(), json
			);
			log.debug("Category cache put for {}: {} rows affected", category.getId(), affectedRows);
		} catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}
	}

	public void evictCategory(UUID key) {
		int affectedRow = cacheJdbcTemplate.update(
				"DELETE FROM category_cache WHERE id=?::uuid", key
		);
		log.debug("Category cache evict for {} :{} rows affected", key, affectedRow);
	}

	public Optional<MachineEntity> getMachineEntity(UUID key) {
		try {
			String json = cacheJdbcTemplate.queryForObject(
					"""
							SELECT value FROM machine_cache WHERE id=?::uuid
							""", String.class, key);
			log.debug("Machine cache hit for {}", key);
			return Optional.of(objectMapper.readValue(json, MachineEntity.class));
		} catch (EmptyResultDataAccessException ex) {
			log.debug("Machine cache miss for {}", key);
			return Optional.empty();
		} catch (Exception ex) {
			throw new RuntimeException("failed to deserialize object", ex);
		}
	}

	public void putMachine(MachineEntity machine) {
		try {
			String json = objectMapper.writeValueAsString(machine);
			int affectedRows = cacheJdbcTemplate.update(
					"""
							INSERT INTO machine_cache (id,value,inserted_at)
							VALUES (?::uuid,?::jsonb,NOW())
							ON CONFLICT (id) DO UPDATE
							SET value = EXCLUDED.value, inserted_at = NOW()
							""",
					machine.getId(), json
			);
			log.debug("Machine cache put for {}: {} rows affected", machine.getId(), affectedRows);
		} catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}
	}

	public void evictMachine(UUID key) {
		int affectedRows = cacheJdbcTemplate.update("DELETE FROM machine_cache WHERE id=?::uuid", key);
		log.debug("Machine cache evict for {}: {} rows removed", key, affectedRows);
	}

	public Optional<List<MaintenanceRecordEntity>> getMaintenanceRecord(UUID machineId) {
		try {
			String json = cacheJdbcTemplate.queryForObject(
					"SELECT value FROM maintenance_records_cache WHERE machine_id=?::uuid", String.class, machineId
			);
			log.debug("Maintenance records cache hit for {}", machineId);
			List<MaintenanceRecordEntity> records = objectMapper.readValue(
					json,
					objectMapper.getTypeFactory()
							.constructCollectionType(List.class, MaintenanceRecordEntity.class)
			);
			return Optional.of(records);
		}catch (EmptyResultDataAccessException ex) {
			log.debug("Maintenance records cache miss for {}", machineId);
			return Optional.empty();
		}
		catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}
	}

	public void putMaintenanceRecord(UUID machineId, MaintenanceRecordEntity maintenanceRecord) {
		try {
			String json = objectMapper.writeValueAsString(maintenanceRecord);
			int affectedRows = cacheJdbcTemplate.update(
					"""
                      INSERT INTO maintenance_records_cache (machine_id,value,inserted_at)
                      VALUES (?::uuid,?::jsonb,NOW())
                      ON CONFLICT (machine_id) DO UPDATE
                      SET value = maintenance_records_cache.value || ?::jsonb, inserted_at = NOW()
                      """,
					machineId,
					"[" + json + "]",
					"[" + json + "]"
			);
			log.debug("Maintenance records cache put for {}: {} rows affected", machineId, affectedRows);
		}catch (Exception ex) {
			throw new RuntimeException("failed to serialize object", ex);
		}
	}
}
