package com.rentitup.api_gateway.dto;

import lombok.Data;

@Data
public class CreateCategoryRequestDto {
	private String name;
	private String description;
	private String iconUrl;
	private PriceCalculationType defaultPriceType;
}
