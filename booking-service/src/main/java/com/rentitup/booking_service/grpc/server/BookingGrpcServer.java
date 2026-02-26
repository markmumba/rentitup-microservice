package com.rentitup.booking_service.grpc.server;

import com.rentitup.booking_service.service.BookingService;
import com.rentitup.booking_service.service.PaymentService;
import com.rentitup.booking_service.service.ReviewService;
import com.rentitup.shared.proto.booking.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingGrpcServer extends BookingServiceGrpc.BookingServiceImplBase {
	private final BookingService bookingService;
	private final PaymentService paymentService;
	private final ReviewService reviewService;


	@Override
	public void createBooking(CreateBookingRequest request, StreamObserver<BookingResponse> responseObserver) {
		super.createBooking(request, responseObserver);
	}

	@Override
	public void getBooking(GetBookingRequest request, StreamObserver<BookingResponse> responseObserver) {
		super.getBooking(request, responseObserver);
	}

	@Override
	public void updateBookingStatus(UpdateBookingStatusRequest request, StreamObserver<BookingResponse> responseObserver) {
		super.updateBookingStatus(request, responseObserver);
	}

	@Override
	public void cancelBooking(CancelBookingRequest request, StreamObserver<BookingResponse> responseObserver) {
		super.cancelBooking(request, responseObserver);
	}

	@Override
	public void listBookingsByCustomer(ListBookingsByCustomerRequest request, StreamObserver<ListBookingsResponse> responseObserver) {
		super.listBookingsByCustomer(request, responseObserver);
	}

	@Override
	public void listBookingsByMachine(ListBookingsByMachineRequest request, StreamObserver<ListBookingsResponse> responseObserver) {
		super.listBookingsByMachine(request, responseObserver);
	}

	@Override
	public void listBookingsForOwner(ListBookingsForOwnerRequest request, StreamObserver<ListBookingsResponse> responseObserver) {
		super.listBookingsForOwner(request, responseObserver);
	}

	@Override
	public void createPayment(CreatePaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
		super.createPayment(request, responseObserver);
	}

	@Override
	public void updatePaymentStatus(UpdatePaymentStatusRequest request, StreamObserver<PaymentResponse> responseObserver) {
		super.updatePaymentStatus(request, responseObserver);
	}

	@Override
	public void getPaymentsByBooking(GetPaymentsByBookingRequest request, StreamObserver<PaymentsResponse> responseObserver) {
		super.getPaymentsByBooking(request, responseObserver);
	}

	@Override
	public void createReview(CreateReviewRequest request, StreamObserver<ReviewResponse> responseObserver) {
		super.createReview(request, responseObserver);
	}

	@Override
	public void getReviewsByMachine(GetReviewsByMachineRequest request, StreamObserver<ReviewsResponse> responseObserver) {
		super.getReviewsByMachine(request, responseObserver);
	}

	@Override
	public void getReviewsByOwner(GetReviewsByOwnerRequest request, StreamObserver<ReviewsResponse> responseObserver) {
		super.getReviewsByOwner(request, responseObserver);
	}
}
