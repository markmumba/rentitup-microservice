package com.rentitup.booking_service.service.impl;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import com.rentitup.booking_service.grpc.client.CatalogGrpcClient;
import com.rentitup.booking_service.grpc.client.UserGrpcClient;
import com.rentitup.booking_service.mapper.BookingMapper;
import com.rentitup.booking_service.repository.BookingRepository;
import com.rentitup.booking_service.service.BookingService;
import com.rentitup.common.exceptions.BadRequestException;
import com.rentitup.common.exceptions.ConflictException;
import com.rentitup.common.exceptions.NotFoundException;
import com.rentitup.common.exceptions.ServiceUnavailableException;
import com.rentitup.shared.proto.booking.CreateBookingRequest;
import com.rentitup.shared.proto.catalog.Machine;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final CatalogGrpcClient catalogGrpcClient;
	private final UserGrpcClient userGrpcClient;
	private final BookingMapper bookingMapper;


	@Override
	public BookingEntity createBooking(CreateBookingRequest request) {
		log.info("Creating booking for machine: {}", request.getMachineId());

		Machine machine;
		try {
			machine = catalogGrpcClient.getMachine(request.getMachineId());

		} catch (StatusRuntimeException e) {
			log.error("Failed to fetch machine from catalog service: {}", e.getStatus());
			if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
				throw new NotFoundException("Machine not found: " + request.getMachineId());
			}
			if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
				throw new BadRequestException("Invalid machine ID: " + request.getMachineId());
			}
			throw new ServiceUnavailableException("Catalog service unavailable: " + e.getStatus().getDescription(), e);
		}
		try {
			userGrpcClient.getUser(request.getCustomerId());
		}
		catch (StatusRuntimeException e) {
			log.error("Failed to fetch user from catalog service: {}", e.getStatus());
			if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
				throw new NotFoundException("Customer not found: " + request.getCustomerId());
			}
			if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
				throw new BadRequestException("Invalid customer ID: " + request.getCustomerId());
			}
		}

		UUID machineId = UUID.fromString(request.getMachineId());
		LocalDate startDate = bookingMapper.fromTimestampToLocalDate(request.getStartDate());
		LocalDate endDate = bookingMapper.fromTimestampToLocalDate(request.getEndDate());

		if (startDate == null || endDate == null) {
			throw new BadRequestException("Start date and end date are required");
		}

		if (endDate.isBefore(startDate)) {
			throw new BadRequestException("End date cannot be before start date");
		}

		boolean hasOverlap = bookingRepository.existsOverlappingBookingForMachine(
				machineId, startDate, endDate
		);

		if (hasOverlap) {
			throw new ConflictException("Machine is already booked for the selected dates");
		}

		BigDecimal basePrice = new BigDecimal(machine.getBasePrice().getAmount());
		long days = ChronoUnit.DAYS.between(startDate, endDate);
		if (days == 0) days = 1;

		BigDecimal totalPrice = switch (machine.getPriceType()) {
			case WEEKLY -> {
				int weeks = request.getWeeks() > 0 ? request.getWeeks() : 1;
				yield basePrice.multiply(BigDecimal.valueOf(weeks));
			}
			case HOURLY -> {
				int hours = request.getHours() > 0 ? request.getHours() : 1;
				yield basePrice.multiply(BigDecimal.valueOf(hours));
			}
			case DISTANCE_BASED -> {
				int distance = request.getDistance() > 0 ? request.getDistance() : 1;
				yield basePrice.multiply(BigDecimal.valueOf(distance));
			}
			default -> basePrice.multiply(BigDecimal.valueOf(days));
		};

		BookingEntity createdBooking = BookingEntity.builder()
				.machineId(machineId)
				.customerId(UUID.fromString(request.getCustomerId()))
				.startDate(startDate)
				.endDate(endDate)
				.pickupLatitude(
						request.getPickupLocation().getLatitude() != 0.0 ?
								BigDecimal.valueOf(request.getPickupLocation().getLatitude()) : null)
				.pickupLongitude(
						request.getPickupLocation().getLongitude() != 0.0 ?
								BigDecimal.valueOf(request.getPickupLocation().getLongitude()) : null)
				.pickupAddress(
						!request.getPickupLocation().getAddress().isEmpty() ?
								request.getPickupLocation().getAddress() : null)
				.status(BookingStatus.PENDING)
				.dailyRate(basePrice)
				.totalAmount(totalPrice)
				.durationDays((int) days)
				.specialRequirements(
						!request.getSpecialRequirements().isEmpty() ?
								request.getSpecialRequirements() : null)
				.build();

		BookingEntity saved = bookingRepository.save(createdBooking);
		log.info("Booking created successfully with id: {}", saved.getId());
		return saved;
	}

	@Override
	public BookingEntity getBookingById(UUID bookingId) {
		return bookingRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
	}

	@Override
	public BookingEntity updateBookingStatus(UUID bookingId, BookingStatus status) {
		log.info("Updating booking status: {}", status);
		BookingEntity bookingToUpdate = getBookingById(bookingId);
		log.info("Checking booking status flow: {} -> {}", bookingToUpdate.getStatus(), status);
		if (!bookingStatusFlow(bookingToUpdate.getStatus(), status)) {
			throw new ConflictException("Invalid booking status transition: " + bookingToUpdate.getStatus() + " -> " + status);
		}
		bookingToUpdate.setStatus(status);
		if (status == BookingStatus.CONFIRMED) {
			bookingToUpdate.setConfirmedAt(Instant.now());
		}
		if (status == BookingStatus.COMPLETED) {
			bookingToUpdate.setCompletedAt(Instant.now());
		}
		BookingEntity saved = bookingRepository.save(bookingToUpdate);
		log.info("Booking status updated successfully with id: {}", saved.getId());
		return saved;
	}

	@Override
	public Page<BookingEntity> getCustomerBookings(UUID customerId, Pageable pageable, BookingStatus status) {
		if (status != null) {
			return bookingRepository.findByCustomerIdAndStatus(customerId, status, pageable);
		}
		return bookingRepository.findByCustomerId(customerId, pageable);
	}

	@Override
	public Page<BookingEntity> getBookingsByMachine(UUID machineId, Pageable pageable) {
		return bookingRepository.findByMachineId(machineId,pageable);
	}

	@Override
	public Page<BookingEntity> getOwnerBookings(UUID ownerId, Pageable pageable, BookingStatus status) {
		List<UUID> machineIds = catalogGrpcClient.getMachineIdsByOwnerId(ownerId.toString());
		return bookingRepository.findByMachineIdIn(machineIds, pageable);
	}

	@Override
	public BookingEntity cancelBooking(UUID bookingId, String reason) {
		BookingEntity bookingToCancel = getBookingById(bookingId);
		if (bookingToCancel.getStatus() == BookingStatus.CANCELLED) {
			throw new BadRequestException("Booking is already cancelled");
		}
		if (!bookingToCancel.getStatus().isCancellable()) {
			throw new ConflictException("Cannot cancel a booking in this status: " + bookingToCancel.getStatus());
		}
		if (bookingToCancel.getAmountPaid() != null || bookingToCancel.getSecurityDeposit() != null) {
			//TODO: refund the amount paid or security deposit
		}

		bookingToCancel.setStatus(BookingStatus.CANCELLED);
		bookingToCancel.setCancellationReason(reason);
		BookingEntity saved = bookingRepository.save(bookingToCancel);
		log.info("Booking cancelled successfully with id: {}", saved.getId());
		return saved;
	}


	private boolean bookingStatusFlow(BookingStatus currentStatus, BookingStatus newStatus) {

		if (currentStatus == BookingStatus.CANCELLED) {
			return false;
		}

		return switch (currentStatus) {
			case PENDING -> newStatus == BookingStatus.CONFIRMED;
			case CONFIRMED -> newStatus == BookingStatus.PAID || newStatus == BookingStatus.CANCELLED;
			case PAID -> newStatus == BookingStatus.ONGOING;
			case ONGOING -> newStatus == BookingStatus.COMPLETED;
			case COMPLETED -> newStatus == BookingStatus.COMPLETED || newStatus == BookingStatus.CANCELLED;
			default -> throw new IllegalArgumentException("Invalid current status: " + currentStatus);
		};
	}
}
