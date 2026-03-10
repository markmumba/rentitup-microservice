package com.rentitup.booking_service.repository;

import com.rentitup.booking_service.entities.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
	List<PaymentEntity> findByBookingId(UUID bookingId);
}
