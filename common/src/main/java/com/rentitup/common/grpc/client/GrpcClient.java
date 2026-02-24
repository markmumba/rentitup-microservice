package com.rentitup.common.grpc.client;

import java.lang.annotation.*;

/**
 * Annotation to inject a gRPC client stub.
 * The stub will be created using the GrpcChannelFactory with service discovery.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * // Basic injection
 * @GrpcClient("USER-SERVICE")
 * private UserServiceGrpc.UserServiceBlockingStub userServiceStub;
 *
 * // With automatic token forwarding
 * @GrpcClient(value = "USER-SERVICE", forwardToken = true)
 * private UserServiceGrpc.UserServiceBlockingStub userServiceStub;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrpcClient {
	/**
	 * The service name as registered in Eureka (e.g., "USER-SERVICE", "CATALOG-SERVICE").
	 */
	String value();

	/**
	 * If true, automatically forwards the current user's bearer token to the downstream service.
	 * The token is retrieved from the Spring Security context.
	 * Default is false.
	 */
	boolean forwardToken() default false;
}
