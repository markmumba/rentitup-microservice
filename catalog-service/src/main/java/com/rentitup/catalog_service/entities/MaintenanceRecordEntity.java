package com.rentitup.catalog_service.entities;



import com.rentitup.catalog_service.common.entites.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;


@Entity
@Table(name = "maintenance_records", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MaintenanceRecordEntity extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "machine_id", nullable = false)
	private MachineEntity machine;

	@Column(name = "service_date", nullable = false)
	private LocalDate serviceDate;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "performed_by")
	private String performedBy;

	@Column(name = "next_service_date")
	private LocalDate nextServiceDate;

	@Column(name = "reminder_sent_at")
	private Instant reminderSentAt;

}