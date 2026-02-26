package com.rentitup.booking_service.service.impl;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import com.rentitup.booking_service.grpc.client.CatalogGrpcClient;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final CatalogGrpcClient catalogGrpcClient;
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
}
