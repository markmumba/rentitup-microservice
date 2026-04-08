package com.rentitup.catalog_service.schedular;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheExpirySchedular {

	private final JdbcTemplate cacheJdbcTemplate;

	public CacheExpirySchedular(@Qualifier("cacheJdbcTemplate")JdbcTemplate cacheJdbcTemplate) {
		this.cacheJdbcTemplate = cacheJdbcTemplate;
	}

	@Scheduled(fixedRate =360000)
	public void evictCache() {
		log.info("Running the cache eviction task");
		try {

			cacheJdbcTemplate.execute("CALL expire_cache('category_cache','60 minutes')");
			cacheJdbcTemplate.execute("CALL expire_cache('machine_cache','60 minutes')");
			cacheJdbcTemplate.execute("CALL expire_cache('machine_images_cache','60 minutes')");
			cacheJdbcTemplate.execute("CALL expire_cache('maintenance_records_cache','60 minutes')");

			log.info("Cache eviction completed");
		}catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}
}
