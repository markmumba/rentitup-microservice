package com.rentitup.booking_service.repository;

import com.rentitup.booking_service.entities.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
}
