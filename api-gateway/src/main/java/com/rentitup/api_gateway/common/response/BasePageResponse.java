package com.rentitup.api_gateway.common.response;

import com.rentitup.api_gateway.common.pagination.PageDto;
import com.rentitup.api_gateway.common.pagination.PaginationDto;
import com.rentitup.api_gateway.common.pagination.PaginationMetadataDto;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;


public class BasePageResponse<T>  extends BaseResponse<PageDto<T>> {

	public BasePageResponse(String message, List<T> data, PaginationDto paginationDto, long totalItems){
		super(
				HttpStatus.OK.value(),
				message,
				PageDto.<T>builder()
						.items(data)
						.metadata(new PaginationMetadataDto(totalItems,paginationDto))
						.build(),
				Instant.now().toString()
		);
	}

	public static <T> BasePageResponse<T> of(List<T> data, PaginationDto paginationDto,long totalItems,String message) {
		return new BasePageResponse<>(message,data,paginationDto,totalItems);
	}
	public static <T> BasePageResponse<T> of(List<T> data, PaginationDto paginationDto,long totalItems) {
		return of(data,paginationDto,totalItems,"Success");
	}

	public static <T> BasePageResponse<T> success(List<T> data, PaginationDto paginationDto, long totalItems) {
		return of(data, paginationDto, totalItems);
	}

	public static <T> BasePageResponse<T> success(List<T> data, PaginationDto paginationDto, long totalItems,
												  String message) {
		return of(data, paginationDto, totalItems, message);
	}
}
