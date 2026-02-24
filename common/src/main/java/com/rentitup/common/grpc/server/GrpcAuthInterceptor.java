package com.rentitup.common.grpc.server;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * gRPC server interceptor that validates JWT tokens and populates the gRPC context
 * with user information.
 *
 * <p>Configure public methods (methods that don't require authentication) via
 * {@link GrpcAuthProperties} or by implementing {@link PublicMethodsProvider}.
 */
public class GrpcAuthInterceptor implements ServerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(GrpcAuthInterceptor.class);

	private static final Metadata.Key<String> AUTHORIZATION_HEADER =
			Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

	private static final Set<String> DEFAULT_PUBLIC_METHODS = Set.of(
			"grpc.health.v1.Health/Check",
			"grpc.health.v1.Health/Watch",
			"grpc.reflection.v1alpha.ServerReflection/ServerReflectionInfo"
	);

	private final JwtDecoder jwtDecoder;
	private final Set<String> publicMethods;

	public GrpcAuthInterceptor(JwtDecoder jwtDecoder, Set<String> publicMethods) {
		this.jwtDecoder = jwtDecoder;
		this.publicMethods = new HashSet<>(DEFAULT_PUBLIC_METHODS);
		if (publicMethods != null) {
			this.publicMethods.addAll(publicMethods);
		}
	}

	public GrpcAuthInterceptor(JwtDecoder jwtDecoder) {
		this(jwtDecoder, Set.of());
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
			ServerCall<ReqT, RespT> call,
			Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {

		String methodName = call.getMethodDescriptor().getFullMethodName();
		boolean isPublicMethod = publicMethods.contains(methodName);

		log.debug("Intercepting call to: {} (public: {})", methodName, isPublicMethod);

		String authHeader = headers.get(AUTHORIZATION_HEADER);
		Context context;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			try {
				Jwt jwt = jwtDecoder.decode(token);
				context = buildAuthenticatedContext(jwt);
				log.debug("Valid token for user: {} (type: {})",
						jwt.getClaimAsString("email"),
						jwt.getClaimAsString("token_type"));

			} catch (JwtException e) {
				log.warn("Invalid JWT token: {}", e.getMessage());

				if (!isPublicMethod) {
					call.close(
							Status.UNAUTHENTICATED.withDescription("Invalid token: " + e.getMessage()),
							new Metadata()
					);
					return new ServerCall.Listener<>() {};
				}

				context = buildUnauthenticatedContext();
			}
		} else {
			if (!isPublicMethod) {
				log.warn("No token provided for protected method: {}", methodName);
				call.close(
						Status.UNAUTHENTICATED.withDescription("Authentication required"),
						new Metadata()
				);
				return new ServerCall.Listener<>() {};
			}

			context = buildUnauthenticatedContext();
		}

		return Contexts.interceptCall(context, call, headers, next);
	}

	private Context buildAuthenticatedContext(Jwt jwt) {
		String userId = jwt.getClaimAsString("user_id");
		String userType = jwt.getClaimAsString("user_role");
		String email = jwt.getClaimAsString("email");
		String tokenType = jwt.getClaimAsString("token_type");

		// Extract roles from token
		Set<String> roles = new HashSet<>();
		Object rolesObj = jwt.getClaim("roles");
		if (rolesObj instanceof Collection<?> rolesList) {
			for (Object role : rolesList) {
				if (role != null) {
					roles.add(role.toString());
				}
			}
		}

		return Context.current()
				.withValue(GrpcAuthContext.USER_ID, userId)
				.withValue(GrpcAuthContext.USER_TYPE, userType)
				.withValue(GrpcAuthContext.USER_EMAIL, email)
				.withValue(GrpcAuthContext.TOKEN_TYPE, tokenType)
				.withValue(GrpcAuthContext.ROLES, roles)
				.withValue(GrpcAuthContext.IS_AUTHENTICATED, Boolean.TRUE);
	}

	private Context buildUnauthenticatedContext() {
		return Context.current()
				.withValue(GrpcAuthContext.IS_AUTHENTICATED, Boolean.FALSE);
	}
}
