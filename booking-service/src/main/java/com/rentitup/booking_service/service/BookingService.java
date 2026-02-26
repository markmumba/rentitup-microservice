package com.rentitup.booking_service.service;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.shared.proto.booking.CreateBookingRequest;

public interface BookingService {
	BookingEntity createBooking(CreateBookingRequest createBookingRequest);
}
