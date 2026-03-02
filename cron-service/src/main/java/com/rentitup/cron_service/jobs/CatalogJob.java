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

	@Scheduled(cron = "0 0 0 * * *")
	public void updateMachineRating() {
		log.info("Start updating of machine rating");
		catalogServices.UpdateMachineRatings();
		log.info("Finished updating of machine rating");
	}

	@Scheduled(cron = "0 0 8 * * *")
	public void sendMaintenanceRecordEmails() {
		log.info("Start sending of maintenance record emails");
		catalogServices.sendUpcomingMaintenanceEmails();
		log.info("Finished of maintenance record emails");
	}
}
