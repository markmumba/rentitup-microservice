package com.rentitup.catalog_service.mapper;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.entities.MachineImageEntity;
import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;
import com.rentitup.catalog_service.enums.MachineCondition;
import com.rentitup.catalog_service.enums.MachineStatus;
import com.rentitup.catalog_service.enums.PriceCalculationType;
import com.rentitup.shared.proto.catalog.*;
import com.rentitup.shared.proto.common.Location;
import com.rentitup.shared.proto.common.Money;
import com.rentitup.shared.proto.common.Timestamp;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Mapper(
		componentModel = MappingConstants.ComponentModel.SPRING,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface CatalogMapper {

	@Mapping(target = "machineCount", source = "machineCount")
	@Mapping(target = "mergeFrom", ignore = true)
	@Mapping(target = "clearField", ignore = true)
	@Mapping(target = "clearOneof", ignore = true)
	@Mapping(target = "unknownFields", ignore = true)
	@Mapping(target = "mergeUnknownFields", ignore = true)
	@Mapping(target = "idBytes", ignore = true)
	@Mapping(target = "nameBytes", ignore = true)
	@Mapping(target = "descriptionBytes", ignore = true)
	@Mapping(target = "iconUrlBytes", ignore = true)
	@Mapping(target = "defaultPriceTypeValue", ignore = true)
	@Mapping(target = "allFields", ignore = true)
	@Mapping(target = "createdAt" , ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target ="mergeUpdatedAt", ignore=true)
	@Mapping(target = "mergeCreatedAt",ignore = true)
	Category toProto(CategoryEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "machines", ignore = true)
	@Mapping(target= "machineCount",ignore = true)
	CategoryEntity toEntity(CreateCategoryRequest request);

	default PriceCalculationType map(com.rentitup.shared.proto.catalog.PriceCalculationType type) {
		if (type == null) {
			return PriceCalculationType.DAILY;
		}
		return switch (type) {
			case HOURLY -> PriceCalculationType.HOURLY;
			case DAILY -> PriceCalculationType.DAILY;
			case WEEKLY -> PriceCalculationType.WEEKLY;
			case DISTANCE_BASED -> PriceCalculationType.DISTANCE_BASED;
			case PRICE_CALCULATION_TYPE_UNSPECIFIED, UNRECOGNIZED -> PriceCalculationType.DAILY;
		};
	}

	default com.rentitup.shared.proto.catalog.PriceCalculationType map(PriceCalculationType type) {
		if (type == null) {
			return com.rentitup.shared.proto.catalog.PriceCalculationType.PRICE_CALCULATION_TYPE_UNSPECIFIED;
		}
		return switch (type) {
			case HOURLY -> com.rentitup.shared.proto.catalog.PriceCalculationType.HOURLY;
			case DAILY -> com.rentitup.shared.proto.catalog.PriceCalculationType.DAILY;
			case WEEKLY -> com.rentitup.shared.proto.catalog.PriceCalculationType.WEEKLY;
			case DISTANCE_BASED -> com.rentitup.shared.proto.catalog.PriceCalculationType.DISTANCE_BASED;
		};
	}



	default Timestamp mapLocalDateTime(LocalDateTime dateTime) {
		if (dateTime == null) {
			return Timestamp.getDefaultInstance();
		}
		Instant instant = dateTime.toInstant(ZoneOffset.UTC);
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}

	default LocalDate mapProtoDate(com.rentitup.shared.proto.common.Date date) {
		if (date == null || (date.getYear() == 0 && date.getMonth() == 0 && date.getDay() == 0)) {
			return null;
		}
		return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
	}

	default com.rentitup.shared.proto.common.Date mapLocalDate(LocalDate date) {
		if (date == null) {
			return com.rentitup.shared.proto.common.Date.getDefaultInstance();
		}
		return com.rentitup.shared.proto.common.Date.newBuilder()
				.setYear(date.getYear())
				.setMonth(date.getMonthValue())
				.setDay(date.getDayOfMonth())
				.build();
	}

	// ==================== Machine Mappings ====================

	default MachineEntity toEntity(CreateMachineRequest request) {
		if (request == null) return null;

		var builder = MachineEntity.builder()
				.ownerId(UUID.fromString(request.getOwnerId()))
				.name(request.getName())
				.description(request.getDescription());

		if (request.hasBasePrice()) {
			builder.basePrice(new BigDecimal(request.getBasePrice().getAmount()));
			if (!request.getBasePrice().getCurrency().isEmpty()) {
				builder.currency(request.getBasePrice().getCurrency());
			}
		}
		if (request.hasPriceType()) {
			builder.priceType(map(request.getPriceType()));
		}
		if (request.hasCondition()) {
			builder.condition(map(request.getCondition()));
		}
		if (request.hasLocation()) {
			builder.latitude(BigDecimal.valueOf(request.getLocation().getLatitude()));
			builder.longitude(BigDecimal.valueOf(request.getLocation().getLongitude()));
			if (!request.getLocation().getAddress().isEmpty()) {
				builder.address(request.getLocation().getAddress());
			}
			if (!request.getLocation().getCity().isEmpty()) {
				builder.city(request.getLocation().getCity());
			}
		}
		if (!request.getSpecificationsMap().isEmpty()) {
			builder.specifications(request.getSpecificationsMap());
		}
		if (request.hasIsAvailable()) {
			builder.available(request.getIsAvailable());
		}

		return builder.build();
	}

	default MachineEntity toEntity(UpdateMachineRequest request) {
		if (request == null) return null;

		// For updates, only set fields that are present in the request
		// The service layer will merge these with the existing entity
		var builder = MachineEntity.builder();

		if (request.hasName()) {
			builder.name(request.getName());
		}
		if (request.hasDescription()) {
			builder.description(request.getDescription());
		}
		if (request.hasBasePrice()) {
			builder.basePrice(new BigDecimal(request.getBasePrice().getAmount()));
			if (!request.getBasePrice().getCurrency().isEmpty()) {
				builder.currency(request.getBasePrice().getCurrency());
			}
		}
		if (request.hasPriceType()) {
			builder.priceType(map(request.getPriceType()));
		}
		if (request.hasCondition()) {
			builder.condition(map(request.getCondition()));
		}
		if (request.hasLocation()) {
			builder.latitude(BigDecimal.valueOf(request.getLocation().getLatitude()));
			builder.longitude(BigDecimal.valueOf(request.getLocation().getLongitude()));
			if (!request.getLocation().getAddress().isEmpty()) {
				builder.address(request.getLocation().getAddress());
			}
			if (!request.getLocation().getCity().isEmpty()) {
				builder.city(request.getLocation().getCity());
			}
		}
		if (!request.getSpecificationsMap().isEmpty()) {
			builder.specifications(request.getSpecificationsMap());
		}
		if (request.hasIsAvailable()) {
			builder.available(request.getIsAvailable());
		}

		return builder.build();
	}

	default Machine toProto(MachineEntity entity) {
		if (entity == null) return null;

		Machine.Builder builder = Machine.newBuilder()
				.setId(entity.getId().toString())
				.setOwnerId(entity.getOwnerId().toString())
				.setCategoryId(entity.getCategory().getId().toString())
				.setName(entity.getName())
				.setBasePrice(Money.newBuilder()
						.setAmount(entity.getBasePrice().toPlainString())
						.setCurrency(entity.getCurrency())
						.build())
				.setPriceType(map(entity.getPriceType()))
				.setCondition(map(entity.getCondition()))
				.setStatus(map(entity.getStatus()))
				.setIsAvailable(entity.isAvailable())
				.setAverageRating(entity.getAverageRating().doubleValue())
				.setTotalReviews(entity.getTotalReviews())
				.setTotalRentals(entity.getTotalRentals())
				.setCategory(toProto(entity.getCategory()));

		if (entity.getDescription() != null) {
			builder.setDescription(entity.getDescription());
		}
		if (entity.getLatitude() != null && entity.getLongitude() != null) {
			Location.Builder locationBuilder = Location.newBuilder()
					.setLatitude(entity.getLatitude().doubleValue())
					.setLongitude(entity.getLongitude().doubleValue());
			if (entity.getAddress() != null) {
				locationBuilder.setAddress(entity.getAddress());
			}
			if (entity.getCity() != null) {
				locationBuilder.setCity(entity.getCity());
			}
			builder.setLocation(locationBuilder.build());
		}
		if (entity.getSpecifications() != null) {
			builder.putAllSpecifications(entity.getSpecifications());
		}
		if (entity.getCreatedAt() != null) {
			builder.setCreatedAt(mapLocalDateTime(entity.getCreatedAt()));
		}
		if (entity.getUpdatedAt() != null) {
			builder.setUpdatedAt(mapLocalDateTime(entity.getUpdatedAt()));
		}

		// Map images
		for (MachineImageEntity image : entity.getImages()) {
			builder.addImages(MachineImage.newBuilder()
					.setId(image.getId().toString())
					.setUrl(image.getUrl())
					.setIsPrimary(image.isPrimary())
					.setDisplayOrder(image.getDisplayOrder())
					.build());
		}

		return builder.build();
	}

	default MaintenanceRecordEntity toEntity(AddMaintenanceRecordRequest request){
		if (request == null) return null;
		var builder = MaintenanceRecordEntity.builder()
				.serviceDate(mapProtoDate(request.getServiceDate()))
				.performedBy(request.getPerformedBy())
				.description(request.getDescription());

		if (request.hasNextServiceDate()) {
			builder.nextServiceDate(mapProtoDate(request.getNextServiceDate()));
		}

		return builder.build();
	}

	default MaintenanceRecord toProto(MaintenanceRecordEntity entity){
		if (entity == null) return null;

		MaintenanceRecord.Builder builder = MaintenanceRecord.newBuilder()
				.setId(entity.getId().toString())
				.setMachineId(entity.getMachine().getId().toString())
				.setServiceDate(mapLocalDate(entity.getServiceDate()));

		if (entity.getDescription() != null) {
			builder.setDescription(entity.getDescription());
		}
		if (entity.getPerformedBy() != null) {
			builder.setPerformedBy(entity.getPerformedBy());
		}
		if (entity.getNextServiceDate() != null) {
			builder.setNextServiceDate(mapLocalDate(entity.getNextServiceDate()));
		}
		if (entity.getCreatedAt() != null) {
			builder.setCreatedAt(mapLocalDateTime(entity.getCreatedAt()));
		}
		if (entity.getReminderSentAt() != null) {
			builder.setReminderSentAt(mapInstant(entity.getReminderSentAt()));
		}

		return builder.build();
	}

	default Timestamp mapInstant(Instant instant) {
		if (instant == null) {
			return Timestamp.getDefaultInstance();
		}
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}
	// ==================== MachineStatus Mappings ====================

	default MachineStatus map(com.rentitup.shared.proto.catalog.MachineStatus status) {
		if (status == null) return null;
		return switch (status) {
			case AVAILABLE -> MachineStatus.AVAILABLE;
			case RENTED -> MachineStatus.RENTED;
			case MAINTENANCE -> MachineStatus.MAINTENANCE;
			case INACTIVE -> MachineStatus.INACTIVE;
			case MACHINE_STATUS_UNSPECIFIED, UNRECOGNIZED -> null;
		};
	}

	default com.rentitup.shared.proto.catalog.MachineStatus map(MachineStatus status) {
		if (status == null) return com.rentitup.shared.proto.catalog.MachineStatus.MACHINE_STATUS_UNSPECIFIED;
		return switch (status) {
			case AVAILABLE -> com.rentitup.shared.proto.catalog.MachineStatus.AVAILABLE;
			case RENTED -> com.rentitup.shared.proto.catalog.MachineStatus.RENTED;
			case MAINTENANCE -> com.rentitup.shared.proto.catalog.MachineStatus.MAINTENANCE;
			case INACTIVE -> com.rentitup.shared.proto.catalog.MachineStatus.INACTIVE;
		};
	}

	// ==================== MachineCondition Mappings ====================

	default MachineCondition map(com.rentitup.shared.proto.catalog.MachineCondition condition) {
		if (condition == null) return null;
		return switch (condition) {
			case EXCELLENT -> MachineCondition.EXCELLENT;
			case GOOD -> MachineCondition.GOOD;
			case FAIR -> MachineCondition.FAIR;
			case MACHINE_CONDITION_UNSPECIFIED, UNRECOGNIZED -> null;
		};
	}

	default com.rentitup.shared.proto.catalog.MachineCondition map(MachineCondition condition) {
		if (condition == null) return com.rentitup.shared.proto.catalog.MachineCondition.MACHINE_CONDITION_UNSPECIFIED;
		return switch (condition) {
			case EXCELLENT -> com.rentitup.shared.proto.catalog.MachineCondition.EXCELLENT;
			case GOOD -> com.rentitup.shared.proto.catalog.MachineCondition.GOOD;
			case FAIR -> com.rentitup.shared.proto.catalog.MachineCondition.FAIR;
		};
	}
}
