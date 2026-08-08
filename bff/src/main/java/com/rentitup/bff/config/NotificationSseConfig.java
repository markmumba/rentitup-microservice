package com.rentitup.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class NotificationSseConfig {

	@Bean("notificationSseTaskScheduler")
	public TaskScheduler notificationSseTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(2);
		scheduler.setThreadNamePrefix("notification-sse-");
		scheduler.setWaitForTasksToCompleteOnShutdown(false);
		return scheduler;
	}
}
