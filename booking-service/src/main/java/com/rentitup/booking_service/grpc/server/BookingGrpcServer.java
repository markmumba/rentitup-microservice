package com.rentitup.booking_service.grpc.server;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import com.rentitup.booking_service.mapper.BookingMapper;
import com.rentitup.booking_service.service.BookingService;
import com.rentitup.booking_service.service.PaymentService;
import com.rentitup.booking_service.service.ReviewService;
import com.rentitup.common.grpc.GrpcExceptionHandler;
import com.rentitup.common.util.PaginationHelper;
import com.rentitup.shared.proto.booking.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
			BookingEntity booking = bookingService.updateBookingStatus(bookingId, status);
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
			UUID ownerId = UUID.fromString(request.getOwnerId());
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());
			BookingStatus status = bookingMapper.mapProtoStatus(request.getStatus());
			Page<BookingEntity> page = bookingService.getOwnerBookings(ownerId, pageable, status);
			ListBookingsResponse.Builder bookingsBuilder = ListBookingsResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));
			page.getContent()
					.forEach(bookingEntity -> bookingsBuilder.addBookings(bookingMapper.toProto(bookingEntity)));
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "listBookingsForOwner");
		}
	}

	@Override
	public void createPayment(CreatePaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {

	}

	@Override
	public void updatePaymentStatus(UpdatePaymentStatusRequest request,
			StreamObserver<PaymentResponse> responseObserver) {
		super.updatePaymentStatus(request, responseObserver);
	}

	@Override
	public void getPaymentsByBooking(GetPaymentsByBookingRequest request,
			StreamObserver<PaymentsResponse> responseObserver) {
		super.getPaymentsByBooking(request, responseObserver);
	}

	@Override
	public void createReview(CreateReviewRequest request, StreamObserver<ReviewResponse> responseObserver) {
		super.createReview(request, responseObserver);
	}

	@Override
	public void getReviewsByMachine(GetReviewsByMachineRequest request,
			StreamObserver<ReviewsResponse> responseObserver) {
		super.getReviewsByMachine(request, responseObserver);
	}

	@Override
	public void getReviewsByOwner(GetReviewsByOwnerRequest request, StreamObserver<ReviewsResponse> responseObserver) {
		super.getReviewsByOwner(request, responseObserver);
	}
}
