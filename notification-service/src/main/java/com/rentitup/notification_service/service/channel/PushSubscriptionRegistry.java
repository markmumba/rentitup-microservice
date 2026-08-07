package com.rentitup.notification_service.service.channel;

import com.rentitup.shared.proto.notification.NotificationEvent;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PushSubscriptionRegistry {

	private final Map<String , Set<ServerCallStreamObserver<NotificationEvent>>> subscribers = new ConcurrentHashMap<>();

	public void subscribe(String userId, StreamObserver<NotificationEvent> responseObserver) {
		ServerCallStreamObserver<NotificationEvent> serverObserver = (ServerCallStreamObserver<NotificationEvent>) responseObserver;

		subscribers.computeIfAbsent(userId,ignored -> ConcurrentHashMap.newKeySet()).add(serverObserver);

		log.info("Registered push subscriber for user {}", userId);

		serverObserver.setOnCancelHandler(() ->removeSubscriber(userId,serverObserver));
	}

	public boolean publish( String userId, NotificationEvent notificationEvent) {
		Set<ServerCallStreamObserver<NotificationEvent>> userSubscribers = subscribers.get(userId);

		if( userSubscribers == null || userSubscribers.isEmpty()) {
			log.info("No active push subscribers for user {}", userId);
			return false;
		}

		boolean delivered = false;

		for (ServerCallStreamObserver<NotificationEvent> observer: userSubscribers) {
			if (observer.isCancelled()) {
				removeSubscriber(userId,observer);
				continue;
			}
			try {
				synchronized (observer) {
					observer.onNext(notificationEvent);
				}
				delivered = true;
			}catch (Exception e) {
				log.warn("Failed to push notification to user {}", userId,e);
				removeSubscriber(userId,observer);
			}

		}
		return delivered;
	}

	public void removeSubscriber(String userId,ServerCallStreamObserver<NotificationEvent> streamObserver) {
		Set<ServerCallStreamObserver<NotificationEvent>>  userSubscribers = subscribers.get(userId);
		if(userSubscribers == null) {
			return;
		}
		userSubscribers.remove(streamObserver);
		if (userSubscribers.isEmpty()) {
			subscribers.remove(userId,userSubscribers);
		}

		log.info("Removed push subscriber for user {}",userId);

	}
}
