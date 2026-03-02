package com.rentitup.cron_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CronServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CronServiceApplication.class, args);
	}

}
