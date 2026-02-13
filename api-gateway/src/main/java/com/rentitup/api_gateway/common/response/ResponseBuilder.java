package com.rentitup.api_gateway.common.response;

import com.rentitup.api_gateway.common.pagination.PaginationDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;


public class ResponseBuilder {
	public static <T> ResponseEntity<BaseResponse<T>> success(T data) {
		return  ResponseEntity.ok(BaseResponse.success("Success",data));
	}
	public static <T> ResponseEntity<BaseResponse<T>> success (String message,T data) {
		return ResponseEntity.ok(BaseResponse.success(message,data));
	}

	public static <T> ResponseEntity<BaseResponse<T>> created (String message,T data) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(BaseResponse.success(HttpStatus.CREATED,message,data));
	}

	public static <T> ResponseEntity<BaseResponse<T>> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(BaseResponse.error(status,message));
	}

	public static <T> ResponseEntity<BaseResponse<T>> badRequest(String message) {
		return error(HttpStatus.BAD_REQUEST,message);
	}

	public static <T> ResponseEntity<BaseResponse<T>> notFound(String message) {
		return error(HttpStatus.NOT_FOUND,message);
	}
	public static <T> ResponseEntity<BaseResponse<T>> unauthorized(String message){
		return error(HttpStatus.UNAUTHORIZED,message);
	}
	public static <T> ResponseEntity<BaseResponse<T>> forbidden(String message) {
		return error(HttpStatus.FORBIDDEN,message);
	}
	public static <T> ResponseEntity<BaseResponse<T>> validationError(String message, T data) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(BaseResponse.of(HttpStatus.BAD_REQUEST, message, data));
	}

	public static <T> ResponseEntity<BasePageResponse<T>> successPageResponse(List<T> data, PaginationDto paginationDto, long totalItems, String message) {
		return ResponseEntity.ok(BasePageResponse.of(data,paginationDto,totalItems,message));
	}

	public static <T> ResponseEntity<BasePageResponse<T>> successPageResponse(List<T> data, PaginationDto paginationDto, long totalItems) {
		return ResponseEntity.ok(BasePageResponse.success(data,paginationDto,totalItems));
	}

}
