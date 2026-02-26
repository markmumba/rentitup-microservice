package com.rentitup.booking_service.repository;

import com.rentitup.booking_service.entities.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
}
