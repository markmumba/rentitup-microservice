package com.rentitup.catalog_service.shared;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;

public interface CacheService {
     Optional<CategoryEntity> getCategory(UUID key);
     void putCategory(CategoryEntity category);
     void evictCategory(UUID key);
     void putMachine(MachineEntity machine);
     Optional<MachineEntity> getMachineEntity(UUID key);
     void evictMachine(UUID key);
     Optional<List<MaintenanceRecordEntity>> getMaintenanceRecord(UUID machineId);
     void putMaintenanceRecord(UUID machineId, MaintenanceRecordEntity maintenanceRecord);
     
}