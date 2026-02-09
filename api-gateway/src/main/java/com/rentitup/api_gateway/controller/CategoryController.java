package com.rentitup.api_gateway.controller;

import com.rentitup.api_gateway.common.response.ResponseBuilder;
import com.rentitup.api_gateway.dto.CategoryDto;
import com.rentitup.api_gateway.dto.CreateCategoryRequestDto;
import com.rentitup.api_gateway.grpc.GrpcChannelFactory;
import com.rentitup.api_gateway.grpc.GrpcStubFactory;
import com.rentitup.api_gateway.mapper.GatewayMapper;
import com.rentitup.shared.proto.catalog.CategoryResponse;
import com.rentitup.shared.proto.catalog.CreateCategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {
	private final GrpcStubFactory grpcStubFactory;
	private final GatewayMapper gatewayMapper;

	@PostMapping
	public ResponseEntity<?> createCategory( @Valid @RequestBody CreateCategoryRequestDto requestDto) {
		log.info("REST : Create category request: {}", requestDto.getName());
		CreateCategoryRequest grpcRequest = gatewayMapper.toCreateCategoryRequest(requestDto);
		CategoryResponse grpcResponse = grpcStubFactory.getCatalogStub().createCategory(grpcRequest);
		CategoryDto response = gatewayMapper.toCategoryDto(grpcResponse.getResponse());
		log.info("REST : Create category response: {}", response);
		return ResponseBuilder.created("Category created successfully", response);
	}
}

