package com.rentitup.bff.controller;

import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.bff.grpc.GrpcStubFactory;
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

	@PostMapping
	public ResponseEntity<?> createCategory( @Valid @RequestBody CreateCategoryRequest request) {
		log.info("REST : Create category request: {}", request.getName());
		CategoryResponse response = grpcStubFactory.getCatalogStub().createCategory(request);
		log.info("REST : Create category response: {}", response);
		return ResponseBuilder.created("Category created successfully", response.getResponse());
	}
}

