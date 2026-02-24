package com.rentitup.common.grpc.client;

import java.lang.annotation.*;

/**
 * Annotation to inject a gRPC client stub.
 * The stub will be created using the GrpcChannelFactory with service discovery.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * @GrpcClient("USER-SERVICE")
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
}
