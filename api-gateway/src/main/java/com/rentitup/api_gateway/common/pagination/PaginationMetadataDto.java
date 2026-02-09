package com.rentitup.api_gateway.common.pagination;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMetadataDto {
	private int currentPage;
	private int totalPages;
	private int totalItems;
	private int limit;
	private boolean first;
	private boolean last;


	public PaginationMetadataDto(long totalItems, PaginationDto paginationDto) {
		this.totalItems = (int) totalItems;
		this.limit = paginationDto.getLimit();
		this.currentPage = paginationDto.getPage();
		this.totalPages = (int) Math.ceil((double) totalItems / limit);
		this.first = this.currentPage == 1;
		this.last = this.currentPage >= this.totalPages;
	}

	public PaginationMetadataDto(long totalItems, PaginationDto paginationDto, boolean isFirst, boolean isLast) {
		this.totalItems = (int) totalItems;
		this.limit = paginationDto.getLimit();
		this.currentPage = paginationDto.getPage();
		this.totalPages = (int) Math.ceil((double) totalItems / limit);
		this.first = isFirst;
		this.last = isLast;
	}

}
