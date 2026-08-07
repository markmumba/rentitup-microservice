package com.rentitup.booking_service.grpc.server;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.entities.PaymentEntity;
import com.rentitup.booking_service.entities.ReviewEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import com.rentitup.booking_service.enums.PaymentType;
import com.rentitup.booking_service.mapper.BookingMapper;
import com.rentitup.booking_service.service.BookingService;
import com.rentitup.booking_service.service.PaymentService;
import com.rentitup.booking_service.service.ReviewService;
import com.rentitup.common.grpc.GrpcExceptionHandler;
import com.rentitup.common.grpc.server.GrpcAuthContext;
import com.rentitup.common.exceptions.ForbiddenException;
import com.rentitup.common.util.PaginationHelper;
import com.rentitup.shared.proto.booking.*;
import com.rentitup.shared.proto.common.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingGrpcServer extends BookingServiceGrpc.BookingServiceImplBase {
	private final BookingService bookingService;
	private final PaymentService paymentService;
	private final ReviewService reviewService;
	private final BookingMapper bookingMapper;

	@Override
	public void createBooking(CreateBookingRequest request, StreamObserver<BookingResponse> responseObserver) {
		try {
			log.info("gRPC: Create booking for machine: {}", request.getMachineId());

			BookingEntity savedBooking = bookingService.createBooking(request);
			BookingResponse response = BookingResponse.newBuilder()
					.setBooking(bookingMapper.toProto(savedBooking))
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "Create booking");
		}
	}

	@Override
	public void getBooking(GetBookingRequest request, StreamObserver<BookingResponse> responseObserver) {
		try {

			log.info("gRPC: Get booking: {}", request.getId());
			UUID bookingId = UUID.fromString(request.getId());
			BookingEntity booking = bookingService.getBookingById(bookingId);
			log.info("gRPC: Booking fetched: {}", booking.getId());
			BookingResponse response = BookingResponse.newBuilder()
					.setBooking(bookingMapper.toProto(booking))
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "getBooking");
		}
	}

	@Override
	public void updateBookingStatus(UpdateBookingStatusRequest request,
			StreamObserver<BookingResponse> responseObserver) {
		try {
			log.info("gRPC: Update booking status: {}", request.getId());
			UUID bookingId = UUID.fromString(request.getId());
			BookingStatus status = bookingMapper.mapProtoStatus(request.getStatus());
			UUID actorId = UUID.fromString(request.getActorId());
			BookingEntity booking = bookingService.updateBookingStatus(
					bookingId, status, actorId, request.getActorIsAdmin());
			BookingResponse response = BookingResponse.newBuilder()
					.setBooking(bookingMapper.toProto(booking))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "updateBookingStatus");
		}
	}

	@Override
	public void cancelBooking(CancelBookingRequest request, StreamObserver<BookingResponse> responseObserver) {
		try{
			log.info("gRPC: Cancel booking: {}", request.getId());
			UUID bookingId = UUID.fromString(request.getId());
			String reason = request.getReason();
			BookingEntity booking = bookingService.cancelBooking(bookingId, reason);
			BookingResponse response = BookingResponse.newBuilder()
					.setBooking(bookingMapper.toProto(booking))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "cancelBooking");
		}	
	}

	@Override
	public void listBookingsByCustomer(ListBookingsByCustomerRequest request,
			StreamObserver<ListBookingsResponse> responseObserver) {
		try {
			log.info("gRPC: List bookings by customer: {}", request.getCustomerId());
			UUID customerId = UUID.fromString(request.getCustomerId());
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());
			BookingStatus status = bookingMapper.mapProtoStatus(request.getStatus());
			Page<BookingEntity> page = bookingService.getCustomerBookings(customerId, pageable, status);
			ListBookingsResponse.Builder bookingsBuilder = ListBookingsResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent()
					.forEach(bookingEntity -> bookingsBuilder.addBookings(bookingMapper.toProto(bookingEntity)));

			responseObserver.onNext(bookingsBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "listBookingsByCustomer");
		}
	}

	@Override
	public void listBookingsByMachine(ListBookingsByMachineRequest request,
			StreamObserver<ListBookingsResponse> responseObserver) {
		try {
			UUID machineId = UUID.fromString(request.getMachineId());
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());
			Page<BookingEntity> page = bookingService.getBookingsByMachine(machineId, pageable);
			ListBookingsResponse.Builder bookingsBuilder = ListBookingsResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent()
					.forEach(bookingEntity -> bookingsBuilder.addBookings(bookingMapper.toProto(bookingEntity)));

			responseObserver.onNext(bookingsBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "listBookingsByMachine");
		}
	}

	@Override
	public void listBookingsForOwner(ListBookingsForOwnerRequest request,
			StreamObserver<ListBookingsResponse> responseObserver) {
		try {
			log.info("gRPC: List bookings for owner: {}", request.getOwnerId());
			UUID ownerId = UUID.fromString(request.getOwnerId());
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());
			BookingStatus status = bookingMapper.mapProtoStatus(request.getStatus());
			Page<BookingEntity> page = bookingService.getOwnerBookings(ownerId, pageable, status);
			ListBookingsResponse.Builder bookingsBuilder = ListBookingsResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));
			page.getContent()
					.forEach(bookingEntity -> bookingsBuilder.addBookings(bookingMapper.toProto(bookingEntity)));

			responseObserver.onNext(bookingsBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "listBookingsForOwner");
		}
	}

	@Override
	public void createPayment(CreatePaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
		try {
			log.info("gRPC: Create payment for booking: {}", request.getBookingId());
			UUID bookingId = UUID.fromString(request.getBookingId());
			UUID customerId = UUID.fromString(request.getCustomerId());
			UUID authenticatedCustomerId = UUID.fromString(GrpcAuthContext.requireUserId());
			if (!authenticatedCustomerId.equals(customerId)) {
				throw new ForbiddenException("A customer can only initiate their own payment");
			}
			BigDecimal amount = new BigDecimal(request.getAmount().getAmount());
			String currency = request.getAmount().getCurrency();
			String transactionId = request.getTransactionId();
			PaymentType type = bookingMapper.mapPaymentType(request.getPaymentType());

			if (type == null) {
				throw new IllegalArgumentException("Payment type is required");
			}

			PaymentEntity payment = paymentService.createPaymentEntry(
					bookingId, customerId, amount, currency, type, transactionId);

			PaymentResponse response = PaymentResponse.newBuilder()
					.setPayment(bookingMapper.toProto(payment))
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "createPayment");
		}
	}

	@Override
	public void updatePaymentStatus(UpdatePaymentStatusRequest request,
			StreamObserver<PaymentResponse> responseObserver) {
		try {
			log.info("gRPC: Update payment status: {}", request.getId());
			if (!GrpcAuthContext.isServiceToken() && !"ADMIN".equals(GrpcAuthContext.getRole())) {
				throw new ForbiddenException("Only a trusted service or admin can update payment status");
			}
			UUID paymentId = UUID.fromString(request.getId());
			com.rentitup.booking_service.enums.PaymentStatus status =
					bookingMapper.mapProtoPaymentStatus(request.getStatus());

			PaymentEntity payment = paymentService.updatePaymentStatus(paymentId, status);

			PaymentResponse response = PaymentResponse.newBuilder()
					.setPayment(bookingMapper.toProto(payment))
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "updatePaymentStatus");
		}
	}

	@Override
	public void getPaymentsByBooking(GetPaymentsByBookingRequest request,
			StreamObserver<PaymentsResponse> responseObserver) {
		try {
			log.info("gRPC: Get payments for booking: {}", request.getBookingId());
			UUID bookingId = UUID.fromString(request.getBookingId());

			var payments = paymentService.getPaymentsByBooking(bookingId);

			PaymentsResponse.Builder responseBuilder = PaymentsResponse.newBuilder();
			payments.forEach(payment -> responseBuilder.addPayments(bookingMapper.toProto(payment)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "getPaymentsByBooking");
		}
	}

	@Override
	public void createReview(CreateReviewRequest request, StreamObserver<ReviewResponse> responseObserver) {
		try {
			log.info("gRPC: Create review for booking: {}", request.getBookingId());
			UUID bookingId = UUID.fromString(request.getBookingId());
			UUID reviewerId = UUID.fromString(request.getReviewerId());

			var review = reviewService.createReview(
					bookingId,
					reviewerId,
					request.getMachineRating(),
					request.getOwnerRating(),
					request.getComment()
			);

			ReviewResponse response = ReviewResponse.newBuilder()
					.setReview(bookingMapper.toProto(review))
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "createReview");
		}
	}

	@Override
	public void getReviewsByMachine(GetReviewsByMachineRequest request,
			StreamObserver<ReviewsResponse> responseObserver) {
		try {
			log.info("gRPC: Get reviews for machine: {}", request.getMachineId());
			UUID machineId = UUID.fromString(request.getMachineId());
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());

			var page = reviewService.getReviewsByMachine(machineId, pageable);

			ReviewsResponse.Builder responseBuilder = ReviewsResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));
			page.getContent().forEach(review -> responseBuilder.addReviews(bookingMapper.toProto(review)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "getReviewsByMachine");
		}
	}

	@Override
	public void getAllReviews(Empty request, StreamObserver<ListReviewResponse> responseObserver) {
		try {
			List<ReviewEntity> reviews = reviewService.getAllReviews();

			ListReviewResponse response = ListReviewResponse.newBuilder()
					.addAllReviews(reviews.stream()
							.map(bookingMapper::toProto)
							.toList()
					)
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();

		}catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "getAllReviews");
		}
	}

	@Override
	public void getReviewsByOwner(GetReviewsByOwnerRequest request, StreamObserver<ReviewsResponse> responseObserver) {
		try {
			log.info("gRPC: Get reviews for owner: {}", request.getOwnerId());
			UUID ownerId = UUID.fromString(request.getOwnerId());
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());

			var page = reviewService.getReviewsByOwner(ownerId, pageable);

			ReviewsResponse.Builder responseBuilder = ReviewsResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));
			page.getContent().forEach(review -> responseBuilder.addReviews(bookingMapper.toProto(review)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "getReviewsByOwner");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void getUnsyncedReviews(Empty request, StreamObserver<ListReviewResponse> responseObserver) {
		try {
			log.info("gRPC: Streaming unsynced reviews");
			final int BATCH_SIZE = 100;
			List<Review> batch = new ArrayList<>();
			AtomicInteger count = new AtomicInteger(0);

			reviewService.streamUnsyncedReviews().forEach(review -> {
				batch.add(bookingMapper.toProto(review));
				count.incrementAndGet();

				if (batch.size() >= BATCH_SIZE) {
					responseObserver.onNext(ListReviewResponse.newBuilder()
							.addAllReviews(batch)
							.build());
					batch.clear();
				}
			});

			// Send remaining reviews
			if (!batch.isEmpty()) {
				responseObserver.onNext(ListReviewResponse.newBuilder()
						.addAllReviews(batch)
						.build());
			}

			log.info("gRPC: Streamed {} unsynced reviews", count.get());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "getUnsyncedReviews");
		}
	}

	@Override
	public void markReviewsSynced(MarkReviewsSyncedRequest request, StreamObserver<MarkReviewsSyncedResponse> responseObserver) {
		try {
			log.info("gRPC: Marking {} reviews as synced", request.getReviewIdsCount());

			List<UUID> reviewIds = request.getReviewIdsList().stream()
					.map(UUID::fromString)
					.toList();

			int updatedCount = reviewService.markReviewsAsSynced(reviewIds);

			MarkReviewsSyncedResponse response = MarkReviewsSyncedResponse.newBuilder()
					.setUpdatedCount(updatedCount)
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "markReviewsSynced");
		}
	}

}
