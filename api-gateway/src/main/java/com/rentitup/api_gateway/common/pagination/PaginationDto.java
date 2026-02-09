package com.rentitup.api_gateway.common.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PaginationDto {
	private int page;
	private int limit;


	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String search;

	public int skip() {
		return (this.page - 1) * this.limit;
	}
}
