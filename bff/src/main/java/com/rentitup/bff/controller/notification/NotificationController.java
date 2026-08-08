package com.rentitup.bff.controller.notification;

import com.rentitup.bff.common.pagination.PaginationDto;
import com.rentitup.bff.common.proto.ProtoJsonUtil;
import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.common.PaginationRequest;
import com.rentitup.shared.proto.notification.ListNotificationsRequest;
import com.rentitup.shared.proto.notification.ListNotificationsResponse;
import com.rentitup.shared.proto.notification.MarkAllNotificationsReadRequest;
import com.rentitup.shared.proto.notification.MarkNotificationReadRequest;
import com.rentitup.shared.proto.notification.NotificationEvent;
import com.rentitup.shared.proto.notification.NotificationServiceGrpc;
import com.rentitup.shared.proto.notification.SubscribeRequest;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.rentitup.bff.common.security.SecurityUtils.requiredCurrentUserId;

@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
public class NotificationController {

	private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(15);

	@GrpcClient("NOTIFICATION-SERVICE")
	private NotificationServiceGrpc.NotificationServiceStub notificationServiceStub;

	@GrpcClient("NOTIFICATION-SERVICE")
	private NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceBlockingStub;

	private final TaskScheduler taskScheduler;

	public NotificationController(@Qualifier("notificationSseTaskScheduler") TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	@GetMapping
	public Object listNotifications(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		ListNotificationsResponse response = notificationServiceBlockingStub.listNotifications(
				ListNotificationsRequest.newBuilder()
						.setUserId(requiredCurrentUserId())
						.setPagination(PaginationRequest.newBuilder()
								.setPage(Math.max(page, 1))
								.setSize(Math.min(Math.max(size, 1), 100))
								.setSortBy("createdAt")
								.setDescending(true)
								.build())
						.build()
		);
		PaginationDto pagination = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();
		return ResponseBuilder.successPageResponse(
				ProtoJsonUtil.toMapList(response.getNotificationsList()),
				pagination,
				response.getPagination().getTotalElements()
		);
	}

	@PatchMapping("/{notificationId}/read")
	public Object markRead(@PathVariable String notificationId) {
		var response = notificationServiceBlockingStub.markNotificationRead(
				MarkNotificationReadRequest.newBuilder()
						.setNotificationId(notificationId)
						.setUserId(requiredCurrentUserId())
						.build()
		);
		return ResponseBuilder.success("Notification marked as read", ProtoJsonUtil.toMap(response.getNotification()));
	}

	@PatchMapping("/read-all")
	public Object markAllRead() {
		var response = notificationServiceBlockingStub.markAllNotificationsRead(
				MarkAllNotificationsReadRequest.newBuilder()
						.setUserId(requiredCurrentUserId())
						.build()
		);
		return ResponseBuilder.success(
				"Notifications marked as read",
				Map.of("updated_count", response.getUpdatedCount())
		);
	}

	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribeToNotifications(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("X-Accel-Buffering", "no");

		String userId = requiredCurrentUserId();
		SseEmitter emitter = new SseEmitter(0L);
		AtomicBoolean closed = new AtomicBoolean(false);
		AtomicReference<ClientCallStreamObserver<SubscribeRequest>> grpcCall = new AtomicReference<>();
		AtomicReference<ScheduledFuture<?>> heartbeat = new AtomicReference<>();

		Runnable cleanup = () -> {
			if (!closed.compareAndSet(false, true)) {
				return;
			}
			ScheduledFuture<?> heartbeatTask = heartbeat.getAndSet(null);
			if (heartbeatTask != null) {
				heartbeatTask.cancel(false);
			}
			ClientCallStreamObserver<SubscribeRequest> call = grpcCall.getAndSet(null);
			if (call != null) {
				call.cancel("SSE client disconnected", null);
			}
		};

		emitter.onCompletion(cleanup);
		emitter.onTimeout(cleanup);
		emitter.onError(error -> cleanup.run());

		try {
			emitter.send(SseEmitter.event().name("connected").data(Map.of("connected", true)));
		} catch (IOException exception) {
			cleanup.run();
			emitter.completeWithError(exception);
			return emitter;
		}

		heartbeat.set(taskScheduler.scheduleAtFixedRate(() -> {
			if (closed.get()) {
				return;
			}
			try {
				emitter.send(SseEmitter.event().name("heartbeat").comment("keep-alive"));
			} catch (IOException | IllegalStateException exception) {
				cleanup.run();
				emitter.completeWithError(exception);
			}
		}, HEARTBEAT_INTERVAL));

		SubscribeRequest subscribeRequest = SubscribeRequest.newBuilder().setUserId(userId).build();
		notificationServiceStub.subscribeNotifications(
				subscribeRequest,
				new ClientResponseObserver<SubscribeRequest, NotificationEvent>() {
					@Override
					public void beforeStart(ClientCallStreamObserver<SubscribeRequest> requestStream) {
						grpcCall.set(requestStream);
						if (closed.get() && grpcCall.compareAndSet(requestStream, null)) {
							requestStream.cancel("SSE client disconnected", null);
						}
					}

					@Override
					public void onNext(NotificationEvent value) {
						if (closed.get()) {
							return;
						}
						try {
							emitter.send(SseEmitter.event()
									.id(value.getNotificationId())
									.name("notification")
									.data(ProtoJsonUtil.toMap(value)));
						} catch (IOException | IllegalStateException exception) {
							cleanup.run();
							emitter.completeWithError(exception);
						}
					}

					@Override
					public void onError(Throwable throwable) {
						boolean clientCancellation = Status.fromThrowable(throwable).getCode() == Status.Code.CANCELLED;
						boolean streamWasOpen = !closed.get();
						cleanup.run();
						if (streamWasOpen) {
							if (!clientCancellation) {
								log.warn("Notification stream failed for user {}", userId, throwable);
							}
							emitter.completeWithError(throwable);
						}
					}

					@Override
					public void onCompleted() {
						cleanup.run();
						emitter.complete();
					}
				}
		);

		return emitter;
	}
}
