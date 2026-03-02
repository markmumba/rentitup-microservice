package com.rentitup.booking_service.mapper;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.entities.PaymentEntity;
import com.rentitup.booking_service.entities.ReviewEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import com.rentitup.booking_service.enums.PaymentMethod;
import com.rentitup.booking_service.enums.PaymentStatus;
import com.rentitup.booking_service.enums.PaymentType;
import com.rentitup.shared.proto.booking.Booking;
import com.rentitup.shared.proto.booking.Payment;
import com.rentitup.shared.proto.booking.Review;
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
	default PaymentType mapPaymentType(com.rentitup.shared.proto.booking.PaymentType type) {
		if (type == null) return null;
		return switch (type) {
			case PAYMENT_TYPE_UNSPECIFIED -> null;
			case DEPOSIT ->  PaymentType.DEPOSIT;
			case SECURITY_DEPOSIT ->   PaymentType.SECURITY_DEPOSIT;
			case FULL_PAYMENT ->   PaymentType.FULL_PAYMENT;
			case UNRECOGNIZED -> null;
		};
	}
	default com.rentitup.shared.proto.booking.PaymentType mapPaymentType(PaymentType type) {
		if (type == null) return null;
		return switch (type) {
			case DEPOSIT ->  com.rentitup.shared.proto.booking.PaymentType.DEPOSIT;
			case SECURITY_DEPOSIT ->   com.rentitup.shared.proto.booking.PaymentType.SECURITY_DEPOSIT;
			case FULL_PAYMENT -> com.rentitup.shared.proto.booking.PaymentType.FULL_PAYMENT;
		};
	}

	default Payment toProto(PaymentEntity entity) {
		if (entity == null) return null;

		Payment.Builder builder = Payment.newBuilder()
				.setId(entity.getId().toString())
				.setBookingId(entity.getBooking().getId().toString())
				.setAmount(mapMoney(entity.getAmount(), entity.getCurrency()))
				.setPaymentType(mapPaymentType(entity.getPaymentType()))
				.setStatus(mapPaymentStatus(entity.getStatus()));

		if (entity.getTransactionId() != null) {
			builder.setIdempotencyKey(entity.getTransactionId());
		}

		if (entity.getCompletedAt() != null) {
			builder.setPaidAt(fromInstantToTimestamp(entity.getCompletedAt()));
		}

		return builder.build();
	}

	default Review toProto(ReviewEntity entity) {
		if (entity == null) return null;

		Review.Builder builder = Review.newBuilder()
				.setId(entity.getId().toString())
				.setBookingId(entity.getBooking().getId().toString())
				.setReviewerId(entity.getReviewerId().toString())
				.setMachineId(entity.getMachineId().toString())
				.setMachineRating(entity.getMachineRating())
				.setOwnerRating(entity.getOwnerRating())
				.setRatingSynced(entity.isRatingSynced());

		if (entity.getComment() != null) {
			builder.setComment(entity.getComment());
		}

		if (entity.getCreatedAt() != null) {
			builder.setCreatedAt(fromLocalDateTimeToTimestamp(entity.getCreatedAt()));
		}

		if (entity.getSyncedAt() != null) {
			builder.setSyncedAt(fromInstantToTimestamp(entity.getSyncedAt()));
		}

		return builder.build();
	}

	default Timestamp fromInstantToTimestamp(Instant instant) {
		if (instant == null) return Timestamp.getDefaultInstance();
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}

	default com.rentitup.shared.proto.booking.PaymentStatus mapPaymentStatus(PaymentStatus status) {
		if (status == null) return com.rentitup.shared.proto.booking.PaymentStatus.PAYMENT_STATUS_UNSPECIFIED;
		return switch (status) {
			case PENDING -> com.rentitup.shared.proto.booking.PaymentStatus.PAYMENT_PENDING;
			case COMPLETED -> com.rentitup.shared.proto.booking.PaymentStatus.PAYMENT_COMPLETED;
			case FAILED -> com.rentitup.shared.proto.booking.PaymentStatus.PAYMENT_FAILED;
		};
	}

	default PaymentStatus mapProtoPaymentStatus(com.rentitup.shared.proto.booking.PaymentStatus status) {
		if (status == null) return PaymentStatus.PENDING;
		return switch (status) {
			case PAYMENT_PENDING -> PaymentStatus.PENDING;
			case PAYMENT_COMPLETED -> PaymentStatus.COMPLETED;
			case PAYMENT_FAILED, PAYMENT_REFUNDED -> PaymentStatus.FAILED;
			case PAYMENT_STATUS_UNSPECIFIED, UNRECOGNIZED -> PaymentStatus.PENDING;
		};
	}

	default com.rentitup.shared.proto.booking.PaymentMethod mapPaymentMethod(PaymentMethod method) {
		if (method == null) return com.rentitup.shared.proto.booking.PaymentMethod.PAYMENT_METHOD_UNSPECIFIED;
		return switch (method) {
			case BANK_TRANSFER -> com.rentitup.shared.proto.booking.PaymentMethod.BANK_TRANSFER;
			case MPESA -> com.rentitup.shared.proto.booking.PaymentMethod.MPESA;
			case CASH -> com.rentitup.shared.proto.booking.PaymentMethod.CASH;
			case CARD -> com.rentitup.shared.proto.booking.PaymentMethod.PAYMENT_METHOD_UNSPECIFIED;
		};
	}

	default PaymentMethod mapProtoPaymentMethod(com.rentitup.shared.proto.booking.PaymentMethod method) {
		if (method == null) return PaymentMethod.MPESA;
		return switch (method) {
			case BANK_TRANSFER -> PaymentMethod.BANK_TRANSFER;
			case MPESA -> PaymentMethod.MPESA;
			case CASH -> PaymentMethod.CASH;
			case PAYMENT_METHOD_UNSPECIFIED, UNRECOGNIZED -> PaymentMethod.MPESA;
		};
	}
}
