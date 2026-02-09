package com.rentitup.api_gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CategoryDto {
	private String id;
	private String name;
	private String description;
	private String iconUrl;
	private String defaultPriceType;
	private int machineCount;
	private Instant createdAt;
}
