package com.rentitup.booking_service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingStatus {
	PENDING(true),
	CONFIRMED(true),
	PAID(true),
	ONGOING(false),
	COMPLETED(false),
	CANCELLED(false),
	REJECTED(false);

	private final boolean cancellable;

}
