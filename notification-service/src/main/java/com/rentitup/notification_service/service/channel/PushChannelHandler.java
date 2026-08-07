package com.rentitup.notification_service.service.channel;

import com.rentitup.notification_service.entity.NotificationEntity;
import com.rentitup.notification_service.entity.NotificationEntity.NotificationChannel;
import com.rentitup.shared.proto.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;


@Component
@Slf4j
@RequiredArgsConstructor
public class PushChannelHandler implements NotificationChannelHandler {


	private final PushSubscriptionRegistry pushSubscriptionRegistry;

	@Override
	public NotificationChannel getChannel() {
		return NotificationChannel.PUSH;
	}

	@Override
	public void send(NotificationEntity notification) throws NotificationSendException {
		if(notification.getUserId() == null) {
			throw new NotificationSendException("A userId is required for push notification");
		}

		String userId = notification.getUserId().toString();

		NotificationEvent event = NotificationEvent.newBuilder()
				.setNotificationId(notification.getId().toString())
				.setSubject(notification.getSubject() != null? notification.getSubject():"")
				.setBody(notification.getBody() != null ? notification.getBody() : "" )
				.putAllData(notification.getData()!= null ?  notification.getData(): Map.of())
				.build();

		boolean deliverd = pushSubscriptionRegistry.publish(userId,event);

		if(!deliverd) {
			throw new NotificationSendException("user has no active push notification");
		}
		log.info(
				"Delivered push notification {} to user {}",
				notification.getId(),
				userId
		);

	}

	@Override
	public boolean validateRecipient(String recipient) {
		if (recipient == null || recipient.isBlank()) {
			return false;
		}

		try {
			UUID.fromString(recipient);
			return true;
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}


}
