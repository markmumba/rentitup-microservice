package com.rentitup.common.grpc.server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration properties for gRPC authentication.
 *
 * <p>Configure in application.yml:
 * <pre>
 * grpc:
 *   auth:
 *     public-methods:
 *       - "rentitup.catalog.CatalogService/ListCategories"
 *       - "rentitup.catalog.CatalogService/GetCategory"
 * </pre>
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "grpc.auth")
public class GrpcAuthProperties {

	/**
	 * Set of gRPC method names that don't require authentication.
	 * Format: "package.ServiceName/MethodName"
	 */
	private Set<String> publicMethods = new HashSet<>();

	/**
	 * Whether to enable the gRPC auth interceptor.
	 * Default is true.
	 */
	private boolean enabled = true;

}
