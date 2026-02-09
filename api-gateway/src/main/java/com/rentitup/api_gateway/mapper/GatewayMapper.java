package com.rentitup.api_gateway.mapper;

import com.rentitup.api_gateway.dto.CategoryDto;
import com.rentitup.api_gateway.dto.CreateCategoryRequestDto;
import com.rentitup.api_gateway.dto.PriceCalculationType;
import com.rentitup.shared.proto.catalog.Category;
import com.rentitup.shared.proto.catalog.CreateCategoryRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
		componentModel = MappingConstants.ComponentModel.SPRING,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)

public interface GatewayMapper {
	CreateCategoryRequest toCreateCategoryRequest(CreateCategoryRequestDto requestDto);

	CategoryDto toCategoryDto(Category category);

	default com.rentitup.shared.proto.catalog.PriceCalculationType toPriceType(PriceCalculationType type) {
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
}
