package com.rentitup.booking_service.grpc.client;

import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.notification.NotificationChannel;
import com.rentitup.shared.proto.notification.NotificationPriority;
import com.rentitup.shared.proto.notification.NotificationServiceGrpc;
import com.rentitup.shared.proto.notification.SendNotificationRequest;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class NotificationGrpcClient {

	@GrpcClient("NOTIFICATION-SERVICE")
	private NotificationServiceGrpc.NotificationServiceBlockingStub client;

	public void sendPush(UUID userId, String templateKey, Map<String, String> data) {
		try {
			client.sendNotification(SendNotificationRequest.newBuilder()
					.setRecipient(userId.toString())
					.setUserId(userId.toString())
					.setChannel(NotificationChannel.PUSH)
					.setPriority(NotificationPriority.NORMAL)
					.setTemplateKey(templateKey)
					.putAllData(data)
					.build());
		} catch (StatusRuntimeException exception) {
			log.warn(
					"Could not send push notification {} to user {}: {}",
					templateKey,
					userId,
					exception.getStatus()
			);
		}
	}
}
