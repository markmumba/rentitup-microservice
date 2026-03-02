package com.rentitup.notification_service.repository;

import com.rentitup.notification_service.entity.NotificationEntity;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByUserId(UUID userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndChannel(UUID userId, NotificationChannel channel, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndStatus(UUID userId, NotificationStatus status, Pageable pageable);

    Page<NotificationEntity> findByStatus(NotificationStatus status, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.status = :status AND n.retryCount < :maxRetries ORDER BY n.priority DESC, n.createdAt ASC")
    List<NotificationEntity> findPendingForRetry(@Param("status") NotificationStatus status, @Param("maxRetries") int maxRetries);

    @Query("SELECT n FROM NotificationEntity n WHERE " +
            "(:userId IS NULL OR n.userId = :userId) AND " +
            "(:channel IS NULL OR n.channel = :channel) AND " +
            "(:status IS NULL OR n.status = :status)")
    Page<NotificationEntity> findWithFilters(
            @Param("userId") UUID userId,
            @Param("channel") NotificationChannel channel,
            @Param("status") NotificationStatus status,
            Pageable pageable
    );
}
