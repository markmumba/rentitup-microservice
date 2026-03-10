package com.rentitup.cron_service.services;

import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.booking.*;
import com.rentitup.shared.proto.catalog.*;
import com.rentitup.shared.proto.common.Date;
import com.rentitup.shared.proto.common.Empty;
import com.rentitup.shared.proto.notification.*;
import com.rentitup.shared.proto.user.GetUserRequest;
import com.rentitup.shared.proto.user.User;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.rentitup.cron_service.constants.NotificationConstants.*;


@Service
@Slf4j
public class CatalogServices {

	@GrpcClient("CATALOG-SERVICE")
	private CatalogServiceGrpc.CatalogServiceBlockingStub catalogClient;

	@GrpcClient("BOOKING-SERVICE")
	private BookingServiceGrpc.BookingServiceBlockingStub bookingClient;

	@GrpcClient("NOTIFICATION-SERVICE")
	private NotificationServiceGrpc.NotificationServiceBlockingStub notificationClient;

	@GrpcClient("USER-SERVICE")
	private UserServiceGrpc.UserServiceBlockingStub userClient;

	private static final int MAINTENANCE_REMINDER_DAYS_AHEAD = 10;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");


	public void updateMachineRatings() {
		log.info("Starting machine rating update job (processing only unsynced reviews)");

		// Get only unsynced reviews
		Iterator<ListReviewResponse> reviewResponseIterator = bookingClient.getUnsyncedReviews(Empty.getDefaultInstance());

		int processedCount = 0;
		List<String> syncedReviewIds = new ArrayList<>();

		while (reviewResponseIterator.hasNext()) {
			List<Review> reviews = reviewResponseIterator.next().getReviewsList();

			for (Review review : reviews) {
				try {
					Machine machine = catalogClient.getMachine(GetMachineRequest.newBuilder()
							.setId(review.getMachineId())
							.build()).getMachine();

					int oldTotalReviews = machine.getTotalReviews();
					double oldAverageRating = machine.getAverageRating();
					int newTotalReviews = oldTotalReviews + 1;
					double newAverageRating = ((oldAverageRating * oldTotalReviews) + review.getMachineRating()) / newTotalReviews;

					newAverageRating = Math.round(newAverageRating * 10.0) / 10.0;

					MachineResponse response = catalogClient.updateMachineRating(UpdateMachineRatingRequest.newBuilder()
							.setMachineId(review.getMachineId())
							.setNewAverageRating(newAverageRating)
							.setTotalReviews(newTotalReviews)
							.build());

					log.info("Updated machine '{}' rating to {} ({} reviews)",
							response.getMachine().getName(), newAverageRating, newTotalReviews);

					syncedReviewIds.add(review.getId());
					processedCount++;

				} catch (Exception e) {
					log.error("Failed to update rating for machine {}: {}", review.getMachineId(), e.getMessage());
				}
			}
		}

		// Mark processed reviews as synced
		if (!syncedReviewIds.isEmpty()) {
			try {
				MarkReviewsSyncedResponse syncResponse = bookingClient.markReviewsSynced(
						MarkReviewsSyncedRequest.newBuilder()
								.addAllReviewIds(syncedReviewIds)
								.build()
				);
				log.info("Marked {} reviews as synced", syncResponse.getUpdatedCount());
			} catch (Exception e) {
				log.error("Failed to mark reviews as synced: {}", e.getMessage());
			}
		}

		log.info("Completed machine rating update job. Processed {} unsynced reviews", processedCount);
	}

	public void sendUpcomingMaintenanceEmails() {
		log.info("Starting maintenance reminder job ({} days ahead)", MAINTENANCE_REMINDER_DAYS_AHEAD);

		// Get only maintenance records that need reminders
		Iterator<ListMaintenanceRecordResponse> maintenanceRecords = catalogClient.getMaintenanceRecordsNeedingReminder(
				GetMaintenanceRecordsNeedingReminderRequest.newBuilder()
						.setDaysAhead(MAINTENANCE_REMINDER_DAYS_AHEAD)
						.build()
		);

		int sentCount = 0;
		List<String> remindedRecordIds = new ArrayList<>();

		while (maintenanceRecords.hasNext()) {
			List<MaintenanceRecord> records = maintenanceRecords.next().getRecordsList();

			for (MaintenanceRecord record : records) {
				try {
					Date recordDate = record.getNextServiceDate();
					if (recordDate.getYear() == 0) {
						continue;
					}

					LocalDate nextServiceDate = LocalDate.of(
							recordDate.getYear(),
							recordDate.getMonth(),
							recordDate.getDay()
					);

					Machine machine = catalogClient.getMachine(GetMachineRequest.newBuilder()
							.setId(record.getMachineId())
							.build()).getMachine();

					User owner = userClient.getUser(GetUserRequest.newBuilder()
							.setId(machine.getOwnerId())
							.build()).getUser();

					String nextServiceDateStr = nextServiceDate.format(DATE_FORMATTER);
					String lastServiceDateStr = formatProtoDate(record.getServiceDate());

					SendNotificationRequest request = SendNotificationRequest.newBuilder()
							.setChannel(NotificationChannel.EMAIL)
							.setRecipient(owner.getEmail())
							.setTemplateKey(MAINTENANCE_REMINDER_TEMPLATE)
							.setPriority(NotificationPriority.NORMAL)
							.setUserId(owner.getId())
							.putData(DATA_OWNER_NAME, owner.getFullName())
							.putData(DATA_MACHINE_NAME, machine.getName())
							.putData(DATA_NEXT_SERVICE_DATE, nextServiceDateStr)
							.putData(DATA_LAST_SERVICE_DATE, lastServiceDateStr)
							.build();

					SendNotificationResponse response = notificationClient.sendNotification(request);
					log.info("Sent maintenance reminder to {} for machine '{}': {}",
							owner.getEmail(), machine.getName(), response.getStatus());

					remindedRecordIds.add(record.getId());
					sentCount++;

				} catch (Exception e) {
					log.error("Failed to process maintenance record {}: {}", record.getId(), e.getMessage());
				}
			}
		}

		// Mark processed records as reminded
		if (!remindedRecordIds.isEmpty()) {
			try {
				MarkMaintenanceRemindedResponse remindedResponse = catalogClient.markMaintenanceReminded(
						MarkMaintenanceRemindedRequest.newBuilder()
								.addAllRecordIds(remindedRecordIds)
								.build()
				);
				log.info("Marked {} maintenance records as reminded", remindedResponse.getUpdatedCount());
			} catch (Exception e) {
				log.error("Failed to mark maintenance records as reminded: {}", e.getMessage());
			}
		}

		log.info("Completed maintenance reminder job. Sent {} reminders", sentCount);
	}

	private String formatProtoDate(Date date) {
		if (date == null || date.getYear() == 0) {
			return "N/A";
		}
		return LocalDate.of(date.getYear(), date.getMonth(), date.getDay()).format(DATE_FORMATTER);
	}
}
