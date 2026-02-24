package com.rentitup.common.grpc.client;

import com.rentitup.common.grpc.GrpcChannelFactory;
import io.grpc.Channel;
import io.grpc.stub.AbstractStub;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * BeanPostProcessor that injects gRPC stubs into fields annotated with @GrpcClient.
 */
@RequiredArgsConstructor
public class GrpcClientBeanPostProcessor implements BeanPostProcessor {

	private static final Logger log = LoggerFactory.getLogger(GrpcClientBeanPostProcessor.class);
	private static final BearerTokenCallCredentials TOKEN_CREDENTIALS = new BearerTokenCallCredentials();

	private final GrpcChannelFactory channelFactory;

	@Override
	public Object postProcessBeforeInitialization(Object bean, @NonNull String beanName) throws BeansException {
		Class<?> clazz = bean.getClass();

		ReflectionUtils.doWithFields(clazz, field -> {
			GrpcClient annotation = field.getAnnotation(GrpcClient.class);
			if (annotation != null) {
				String serviceName = annotation.value();
				boolean forwardToken = annotation.forwardToken();

				Object stub = createStub(field.getType(), serviceName, forwardToken);

				if (stub != null) {
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, bean, stub);
					log.debug("Injected gRPC client stub for service '{}' into {}.{} (forwardToken={})",
							serviceName, clazz.getSimpleName(), field.getName(), forwardToken);
				}
			}
		});

		return bean;
	}

	private Object createStub(Class<?> stubClass, String serviceName, boolean forwardToken) {
		try {
			Channel channel = channelFactory.getChannel(serviceName);

			// Find the enclosing gRPC service class (e.g., UserServiceGrpc)
			Class<?> grpcServiceClass = stubClass.getEnclosingClass();
			if (grpcServiceClass == null) {
				log.error("Could not find enclosing gRPC service class for stub: {}", stubClass.getName());
				return null;
			}

			// Determine which factory method to call based on stub type
			String factoryMethodName = getFactoryMethodName(stubClass);
			if (factoryMethodName == null) {
				log.error("Unsupported stub type: {}", stubClass.getName());
				return null;
			}

			Method factoryMethod = grpcServiceClass.getMethod(factoryMethodName, Channel.class);
			Object stub = factoryMethod.invoke(null, channel);

			// Apply token forwarding if requested
			if (forwardToken && stub instanceof AbstractStub<?> abstractStub) {
				stub = abstractStub.withCallCredentials(TOKEN_CREDENTIALS);
			}

			return stub;

		} catch (Exception e) {
			log.error("Failed to create gRPC stub for service '{}': {}", serviceName, e.getMessage(), e);
			return null;
		}
	}

	private String getFactoryMethodName(Class<?> stubClass) {
		String simpleName = stubClass.getSimpleName();

		if (simpleName.endsWith("BlockingStub")) {
			return "newBlockingStub";
		} else if (simpleName.endsWith("FutureStub")) {
			return "newFutureStub";
		} else if (simpleName.endsWith("Stub") && !simpleName.contains("Blocking") && !simpleName.contains("Future")) {
			return "newStub";
		}

		return null;
	}
}
