package com.rentitup.booking_service.service;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import com.rentitup.shared.proto.booking.CreateBookingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingService {
	BookingEntity createBooking(CreateBookingRequest createBookingRequest);

	BookingEntity getBookingById(UUID bookingId);

	BookingEntity updateBookingStatus(UUID bookingId, BookingStatus status, UUID actorId, boolean actorIsAdmin);

	Page<BookingEntity> getCustomerBookings(UUID customerId, Pageable pageable, BookingStatus status);

	Page<BookingEntity> getBookingsByMachine(UUID machineId, Pageable pageable);

	Page<BookingEntity> getOwnerBookings(UUID ownerId, Pageable pageable, BookingStatus status);

	BookingEntity cancelBooking(UUID bookingId, String reason);
}
