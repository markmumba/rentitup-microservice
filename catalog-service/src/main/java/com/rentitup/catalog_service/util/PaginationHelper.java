package com.rentitup.catalog_service.util;

import com.rentitup.shared.proto.common.PaginationRequest;
import com.rentitup.shared.proto.common.PaginationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RequiredArgsConstructor
public final class PaginationHelper {

	public static Pageable toPageable(PaginationRequest request) {
		int page = Math.max(0,request.getPage());
		int size = request.getSize() > 0 ? request.getSize() : 20;

		String sortBy = request.getSortBy();
		if (sortBy != null || sortBy.isBlank()) {
			sortBy = "createdAt";
		}

		Sort sort = request.getDescending() ?
				Sort.by(Sort.Direction.DESC,sortBy)
				: Sort.by(Sort.Direction.ASC,sortBy);

		return PageRequest.of(page, size, sort);
	}

	public static PaginationResponse toProto(Page<?> page) {
		return PaginationResponse.newBuilder()
				.setCurrentPage(page.getNumber())
				.setTotalPages(page.getTotalPages())
				.setTotalElements(page.getTotalElements())
				.setPageSize(page.getSize())
				.setHasNext(page.hasNext())
				.setHasPrevious(page.hasPrevious())
				.build();
	}
}
