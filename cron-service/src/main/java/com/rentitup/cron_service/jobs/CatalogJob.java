package com.rentitup.cron_service.jobs;

import com.rentitup.cron_service.services.CatalogServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogJob {

	private final CatalogServices catalogServices;

	@Scheduled(cron = "${cron.update-ratings:0 0 2 * * *}")
	public void updateMachineRatings() {
		log.info("=== Starting machine rating update job ===");
		try {
			catalogServices.updateMachineRatings();
			log.info("=== Completed machine rating update job ===");
		} catch (Exception e) {
			log.error("=== Machine rating update job FAILED ===", e);
		}
	}

	@Scheduled(cron = "${cron.maintenance-reminder:0 0 8 * * *}")
	public void sendMaintenanceReminders() {
		log.info("=== Starting maintenance reminder job ===");
		try {
			catalogServices.sendUpcomingMaintenanceEmails();
			log.info("=== Completed maintenance reminder job ===");
		} catch (Exception e) {
			log.error("=== Maintenance reminder job FAILED ===", e);
		}
	}
}
