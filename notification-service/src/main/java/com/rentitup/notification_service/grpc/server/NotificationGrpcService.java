package com.rentitup.notification_service.grpc.server;

import com.rentitup.common.grpc.GrpcExceptionHandler;
import com.rentitup.common.util.PaginationHelper;
import com.rentitup.notification_service.entity.NotificationEntity;
import com.rentitup.notification_service.service.NotificationService;
import com.rentitup.notification_service.template.NotificationTemplateService;
import com.rentitup.notification_service.template.TemplateDefinition;
import com.rentitup.shared.proto.notification.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {

	private final NotificationService notificationService;
	private final NotificationTemplateService templateService;

	@Override
	public void sendNotification(SendNotificationRequest request, StreamObserver<SendNotificationResponse> responseObserver) {
		try {
			log.info("gRPC: SendNotification to {} using template {}", request.getRecipient(), request.getTemplateKey());

			NotificationEntity notification = notificationService.send(
					request.getRecipient(),
					mapChannel(request.getChannel()),
					request.getTemplateKey(),
					request.getDataMap(),
					request.hasUserId() ? UUID.fromString(request.getUserId()) : null,
					request.hasPriority() ? mapPriority(request.getPriority()) : null
			);

			SendNotificationResponse response = SendNotificationResponse.newBuilder()
					.setNotificationId(notification.getId().toString())
					.setStatus(mapStatus(notification.getStatus()))
					.setMessage("Notification queued successfully")
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "SendNotification");
		}
	}

	@Override
	public void sendBulkNotification(SendBulkNotificationRequest request, StreamObserver<SendBulkNotificationResponse> responseObserver) {
		try {
			log.info("gRPC: SendBulkNotification with {} notifications", request.getNotificationsCount());

			List<NotificationService.NotificationRequest> requests = request.getNotificationsList().stream()
					.map(this::mapToServiceRequest)
					.toList();

			List<NotificationEntity> results = notificationService.sendBulk(requests);

			int successful = (int) results.stream()
					.filter(n -> n.getStatus() != NotificationEntity.NotificationStatus.FAILED)
					.count();

			SendBulkNotificationResponse.Builder responseBuilder = SendBulkNotificationResponse.newBuilder()
					.setTotal(results.size())
					.setSuccessful(successful)
					.setFailed(results.size() - successful);

			for (NotificationEntity notification : results) {
				responseBuilder.addResults(SendNotificationResponse.newBuilder()
						.setNotificationId(notification.getId().toString())
						.setStatus(mapStatus(notification.getStatus()))
						.setMessage(notification.getErrorMessage() != null ? notification.getErrorMessage() : "Success")
						.build());
			}

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "SendBulkNotification");
		}
	}

	@Override
	public void broadcastNotification(BroadcastNotificationRequest request, StreamObserver<BroadcastNotificationResponse> responseObserver) {
		try {
			log.info("gRPC: BroadcastNotification to {} recipients", request.getRecipientsCount());

			String batchId = notificationService.broadcast(
					request.getRecipientsList(),
					mapChannel(request.getChannel()),
					request.getTemplateKey(),
					request.getDataMap(),
					request.hasPriority() ? mapPriority(request.getPriority()) : null
			);

			BroadcastNotificationResponse response = BroadcastNotificationResponse.newBuilder()
					.setTotal(request.getRecipientsCount())
					.setQueued(request.getRecipientsCount())
					.setBatchId(batchId)
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "BroadcastNotification");
		}
	}

	@Override
	public void getNotification(GetNotificationRequest request, StreamObserver<GetNotificationResponse> responseObserver) {
		try {
			UUID notificationId = UUID.fromString(request.getNotificationId());
			NotificationEntity notification = notificationService.getNotification(notificationId);

			GetNotificationResponse response = GetNotificationResponse.newBuilder()
					.setNotification(mapToProto(notification))
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "GetNotification");
		}
	}

	@Override
	public void listNotifications(ListNotificationsRequest request, StreamObserver<ListNotificationsResponse> responseObserver) {
		try {
			Pageable pageable = PaginationHelper.toPageable(request.getPagination());

			UUID userId = request.hasUserId() ? UUID.fromString(request.getUserId()) : null;
			NotificationEntity.NotificationChannel channel = request.hasChannel() ? mapChannel(request.getChannel()) : null;
			NotificationEntity.NotificationStatus status = request.hasStatus() ? mapEntityStatus(request.getStatus()) : null;

			Page<NotificationEntity> page = notificationService.listNotifications(userId, channel, status, pageable);

			ListNotificationsResponse.Builder responseBuilder = ListNotificationsResponse.newBuilder()
					.setPagination(PaginationHelper.toProto(page));

			page.getContent().forEach(notification ->
					responseBuilder.addNotifications(mapToProto(notification)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "ListNotifications");
		}
	}

	@Override
	public void retryNotification(RetryNotificationRequest request, StreamObserver<RetryNotificationResponse> responseObserver) {
		try {
			UUID notificationId = UUID.fromString(request.getNotificationId());
			NotificationEntity notification = notificationService.retry(notificationId);

			RetryNotificationResponse response = RetryNotificationResponse.newBuilder()
					.setSuccess(true)
					.setMessage("Notification queued for retry")
					.setNewStatus(mapStatus(notification.getStatus()))
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "RetryNotification");
		}
	}

	@Override
	public void getTemplate(GetTemplateRequest request, StreamObserver<GetTemplateResponse> responseObserver) {
		try {
			TemplateDefinition template = templateService.getTemplate(
					request.getKey(),
					mapChannel(request.getChannel())
			).orElseThrow(() -> new IllegalArgumentException("Template not found: " + request.getKey()));

			GetTemplateResponse response = GetTemplateResponse.newBuilder()
					.setTemplate(mapToProto(template))
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "GetTemplate");
		}
	}

	@Override
	public void listTemplates(ListTemplatesRequest request, StreamObserver<ListTemplatesResponse> responseObserver) {
		try {
			NotificationEntity.NotificationChannel channel = request.hasChannel() ? mapChannel(request.getChannel()) : null;
			List<TemplateDefinition> templates = templateService.listTemplates(channel);

			ListTemplatesResponse.Builder responseBuilder = ListTemplatesResponse.newBuilder();
			templates.stream()
					.filter(t -> !request.hasActiveOnly() || !request.getActiveOnly() || t.active())
					.forEach(t -> responseBuilder.addTemplates(mapToProto(t)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			GrpcExceptionHandler.handleException(e, responseObserver, "ListTemplates");
		}
	}

	// ==================== Mapping Methods ====================

	private NotificationService.NotificationRequest mapToServiceRequest(SendNotificationRequest request) {
		return new NotificationService.NotificationRequest(
				request.getRecipient(),
				mapChannel(request.getChannel()),
				request.getTemplateKey(),
				request.getDataMap(),
				request.hasUserId() ? UUID.fromString(request.getUserId()) : null,
				request.hasPriority() ? mapPriority(request.getPriority()) : null
		);
	}

	private NotificationEntity.NotificationChannel mapChannel(NotificationChannel channel) {
		return switch (channel) {
			case EMAIL -> NotificationEntity.NotificationChannel.EMAIL;
			case SMS -> NotificationEntity.NotificationChannel.SMS;
			case PUSH -> NotificationEntity.NotificationChannel.PUSH;
			default -> throw new IllegalArgumentException("Unknown channel: " + channel);
		};
	}

	private NotificationChannel mapChannel(NotificationEntity.NotificationChannel channel) {
		return switch (channel) {
			case EMAIL -> NotificationChannel.EMAIL;
			case SMS -> NotificationChannel.SMS;
			case PUSH -> NotificationChannel.PUSH;
		};
	}

	private NotificationEntity.NotificationPriority mapPriority(NotificationPriority priority) {
		return switch (priority) {
			case LOW -> NotificationEntity.NotificationPriority.LOW;
			case NORMAL -> NotificationEntity.NotificationPriority.NORMAL;
			case HIGH -> NotificationEntity.NotificationPriority.HIGH;
			case URGENT -> NotificationEntity.NotificationPriority.URGENT;
			default -> NotificationEntity.NotificationPriority.NORMAL;
		};
	}

	private NotificationStatus mapStatus(NotificationEntity.NotificationStatus status) {
		return switch (status) {
			case PENDING -> NotificationStatus.PENDING;
			case SENT -> NotificationStatus.SENT;
			case FAILED -> NotificationStatus.FAILED;
			case DELIVERED -> NotificationStatus.DELIVERED;
		};
	}

	private NotificationEntity.NotificationStatus mapEntityStatus(NotificationStatus status) {
		return switch (status) {
			case PENDING -> NotificationEntity.NotificationStatus.PENDING;
			case SENT -> NotificationEntity.NotificationStatus.SENT;
			case FAILED -> NotificationEntity.NotificationStatus.FAILED;
			case DELIVERED -> NotificationEntity.NotificationStatus.DELIVERED;
			default -> null;
		};
	}

	private Notification mapToProto(NotificationEntity entity) {
		Notification.Builder builder = Notification.newBuilder()
				.setId(entity.getId().toString())
				.setRecipient(entity.getRecipient())
				.setChannel(mapChannel(entity.getChannel()))
				.setTemplateKey(entity.getTemplateKey())
				.setStatus(mapStatus(entity.getStatus()))
				.setRetryCount(entity.getRetryCount());

		if (entity.getUserId() != null) {
			builder.setUserId(entity.getUserId().toString());
		}
		if (entity.getData() != null) {
			builder.putAllData(entity.getData());
		}
		if (entity.getSubject() != null) {
			builder.setSubject(entity.getSubject());
		}
		if (entity.getBody() != null) {
			builder.setBody(entity.getBody());
		}
		if (entity.getErrorMessage() != null) {
			builder.setErrorMessage(entity.getErrorMessage());
		}

		return builder.build();
	}

	private NotificationTemplate mapToProto(TemplateDefinition template) {
		return NotificationTemplate.newBuilder()
				.setKey(template.key())
				.setName(template.name() != null ? template.name() : "")
				.setDescription(template.description() != null ? template.description() : "")
				.setChannel(mapChannel(template.channel()))
				.setSubjectTemplate(template.subjectTemplate() != null ? template.subjectTemplate() : "")
				.setBodyTemplate(template.bodyTemplate() != null ? template.bodyTemplate() : "")
				.addAllRequiredFields(template.requiredFields())
				.setIsActive(template.active())
				.build();
	}
}
