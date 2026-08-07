package com.rentitup.bff.controller.notification;

import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.notification.NotificationEvent;
import com.rentitup.shared.proto.notification.NotificationServiceGrpc;
import com.rentitup.shared.proto.notification.SubscribeRequest;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import jakarta.ws.rs.sse.Sse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static com.rentitup.bff.common.security.SecurityUtils.getCurrentUserId;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

	@GrpcClient("NOTIFICATION-SERVICE")
	private NotificationServiceGrpc.NotificationServiceStub notificationServiceStub;


	@GetMapping(value = "/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribeToNotifications () {
		SseEmitter emitter = new SseEmitter(30L);

		String userId = getCurrentUserId();

		SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder()
				.setUserId(userId)
				.build();

		AtomicReference<ClientCallStreamObserver<SubscribeRequest>> grpcCall = new AtomicReference<>();

		ClientResponseObserver<SubscribeRequest, NotificationEvent> observer =
				new ClientResponseObserver<SubscribeRequest, NotificationEvent>() {
					@Override
					public void beforeStart(ClientCallStreamObserver<SubscribeRequest> requestStream) {
						grpcCall.set(requestStream);

					}

					@Override
					public void onNext(NotificationEvent value) {
						try {
							emitter.send(
									SseEmitter.event()
											.name("Notification")
											.data(value)
							);
						} catch (IOException e) {
							cancelGrpcCall(grpcCall);
							emitter.completeWithError(e);

						}
					}

					@Override
					public void onError(Throwable t) {
						emitter.completeWithError(t);
					}

					@Override
					public void onCompleted() {
						emitter.complete();
					}
				};
		notificationServiceStub.subscribeNotifications(subscribeRequest,observer);
		Runnable cleanup = () -> cancelGrpcCall(grpcCall);

		emitter.onCompletion(cleanup);
		emitter.onTimeout(cleanup);
		emitter.onError(error -> cleanup.run());

		return emitter;
	}

	private void cancelGrpcCall(AtomicReference<ClientCallStreamObserver<SubscribeRequest>> grpcCall) {
		ClientCallStreamObserver<SubscribeRequest> call = grpcCall.getAndSet(null);
		if (call != null) {
			call.cancel("SSE client disconnected",null);
		}
	}

}
