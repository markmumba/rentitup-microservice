package com.rentitup.catalog_service.mapper;

import com.rentitup.catalog_service.entities.CategoryEntity;
import com.rentitup.catalog_service.enums.PriceCalculationType;
import com.rentitup.shared.proto.catalog.Category;
import com.rentitup.shared.proto.catalog.CreateCategoryRequest;
import com.rentitup.shared.proto.common.Timestamp;
import org.mapstruct.*;

import java.time.Instant;

@Mapper(
		componentModel = MappingConstants.ComponentModel.SPRING,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface CatalogMapper {

	@Mapping(target = "id", expression = "java(entity.getId().toString())")
	@Mapping(target = "machineCount", expression = "java(entity.getMachineCount())")
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
	Category toProto(CategoryEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "machines", ignore = true)
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

	default Timestamp map(Instant instant) {
		if (instant == null) {
			return Timestamp.getDefaultInstance();
		}
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}

	default Instant map(com.rentitup.shared.proto.common.Timestamp timestamp) {
		if (timestamp == null || timestamp.getSeconds() == 0) {
			return null;
		}
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}

}
