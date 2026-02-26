package com.rentitup.booking_service.mapper;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import com.rentitup.shared.proto.booking.Booking;
import com.rentitup.shared.proto.common.Location;
import com.rentitup.shared.proto.common.Money;
import com.rentitup.shared.proto.common.Timestamp;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(
		componentModel = MappingConstants.ComponentModel.SPRING,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface BookingMapper {

	default Booking toProto(BookingEntity entity) {
		if (entity == null) return null;

		Booking.Builder builder = Booking.newBuilder()
				.setId(entity.getId().toString())
				.setMachineId(entity.getMachineId().toString())
				.setCustomerId(entity.getCustomerId().toString())
				.setStartDate(fromLocalDateToTimestamp(entity.getStartDate()))
				.setEndDate(fromLocalDateToTimestamp(entity.getEndDate()))
				.setStatus(mapStatus(entity.getStatus()))
				.setTotalAmount(mapMoney(entity.getTotalAmount(), entity.getCurrency()));

		if (entity.getPickupLatitude() != null || entity.getPickupLongitude() != null) {
			builder.setPickupLocation(mapLocation(entity));
		}

		if (entity.getSpecialRequirements() != null) {
			builder.setSpecialRequirements(entity.getSpecialRequirements());
		}

		if (entity.getCreatedAt() != null) {
			builder.setCreatedAt(fromLocalDateTimeToTimestamp(entity.getCreatedAt()));
		}

		return builder.build();
	}

	default LocalDate fromTimestampToLocalDate(Timestamp timestamp) {
		if (timestamp == null || timestamp.getSeconds() == 0) return null;
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
				.atZone(ZoneOffset.UTC)
				.toLocalDate();
	}

	default Timestamp fromLocalDateToTimestamp(LocalDate localDate) {
		if (localDate == null) return Timestamp.getDefaultInstance();
		Instant instant = localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}

	default Timestamp fromLocalDateTimeToTimestamp(LocalDateTime dateTime) {
		if (dateTime == null) return Timestamp.getDefaultInstance();
		Instant instant = dateTime.toInstant(ZoneOffset.UTC);
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}

	default Location mapLocation(BookingEntity entity) {
		Location.Builder builder = Location.newBuilder();

		if (entity.getPickupLatitude() != null) {
			builder.setLatitude(entity.getPickupLatitude().doubleValue());
		}
		if (entity.getPickupLongitude() != null) {
			builder.setLongitude(entity.getPickupLongitude().doubleValue());
		}
		if (entity.getPickupAddress() != null) {
			builder.setAddress(entity.getPickupAddress());
		}

		return builder.build();
	}

	default Money mapMoney(BigDecimal amount, String currency) {
		if (amount == null) return Money.getDefaultInstance();
		return Money.newBuilder()
				.setAmount(amount.toPlainString())
				.setCurrency(currency != null ? currency : "KES")
				.build();
	}

	default com.rentitup.shared.proto.booking.BookingStatus mapStatus(BookingStatus status) {
		if (status == null) return com.rentitup.shared.proto.booking.BookingStatus.BOOKING_STATUS_UNSPECIFIED;
		return switch (status) {
			case PENDING -> com.rentitup.shared.proto.booking.BookingStatus.BOOKING_PENDING;
			case CONFIRMED, PAID -> com.rentitup.shared.proto.booking.BookingStatus.BOOKING_CONFIRMED;
			case ONGOING -> com.rentitup.shared.proto.booking.BookingStatus.BOOKING_ONGOING;
			case COMPLETED -> com.rentitup.shared.proto.booking.BookingStatus.BOOKING_COMPLETED;
			case CANCELLED, REJECTED -> com.rentitup.shared.proto.booking.BookingStatus.BOOKING_CANCELLED;
		};
	}

	default BookingStatus mapProtoStatus(com.rentitup.shared.proto.booking.BookingStatus status) {
		if (status == null) return BookingStatus.PENDING;
		return switch (status) {
			case BOOKING_PENDING -> BookingStatus.PENDING;
			case BOOKING_CONFIRMED -> BookingStatus.CONFIRMED;
			case BOOKING_ONGOING -> BookingStatus.ONGOING;
			case BOOKING_COMPLETED -> BookingStatus.COMPLETED;
			case BOOKING_CANCELLED -> BookingStatus.CANCELLED;
			case BOOKING_STATUS_UNSPECIFIED, UNRECOGNIZED -> BookingStatus.PENDING;
		};
	}
}
