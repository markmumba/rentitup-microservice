package com.rentitup.common.grpc.server;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

		log.info("[gRPC-SERVER] Incoming call: {} (public: {})", methodName, isPublicMethod);

		String authHeader = headers.get(AUTHORIZATION_HEADER);
		Context context;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			String tokenPreview = token.length() > 20 ? token.substring(0, 20) + "..." : token;
			log.info("[gRPC-SERVER] Token received: {}", tokenPreview);

			try {
				Jwt jwt = jwtDecoder.decode(token);
				context = buildAuthenticatedContext(jwt, token);

				log.info("[gRPC-SERVER] Authenticated: email={}, type={}, userId={}",
						jwt.getClaimAsString("email"),
						jwt.getClaimAsString("token_type"),
						jwt.getClaimAsString("user_id"));

			} catch (JwtException e) {
				log.warn("[gRPC-SERVER] Invalid JWT token: {}", e.getMessage());

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
			log.info("[gRPC-SERVER] No token in request for: {}", methodName);

			if (!isPublicMethod) {
				log.warn("[gRPC-SERVER] Rejecting unauthenticated request to protected method: {}", methodName);
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

	private Context buildAuthenticatedContext(Jwt jwt,String token) {
		String userId = jwt.getClaimAsString("user_id");
		String role = jwt.getClaimAsString("role");
		String email = jwt.getClaimAsString("email");
		String tokenType = jwt.getClaimAsString("token_type");

		Context initial = Context.current()
				.withValue(GrpcAuthContext.TOKEN, token)
				.withValue(GrpcAuthContext.TOKEN_TYPE, tokenType)
				.withValue(GrpcAuthContext.IS_AUTHENTICATED,Boolean.TRUE);

		if ("USER".equals(tokenType)){
			initial
					.withValue(GrpcAuthContext.USER_ID, userId)
					.withValue(GrpcAuthContext.ROLE, role)
					.withValue(GrpcAuthContext.USER_EMAIL, email);

			log.debug("Authenticated user: {} ({})",
					jwt.getClaimAsString("email"),
					jwt.getClaimAsString("user_type"));

		}else if ("SERVER".equals(tokenType)){
			initial
					.withValue(GrpcAuthContext.CLIENT_ID, userId);
			log.debug("Authenticated service: {}", jwt.getClaimAsString("client_id"));
		}

		Set<String> roles = new HashSet<>();
		Object rolesObj = jwt.getClaim("roles");
		if (rolesObj instanceof Collection<?> rolesList) {
			for (Object r : rolesList) {
				if (r != null) {
					roles.add(r.toString());
				}
			}
		}

		initial.withValue(GrpcAuthContext.ROLES, roles);
		return initial;
	}

	private Context buildUnauthenticatedContext() {
		return Context.current()
				.withValue(GrpcAuthContext.IS_AUTHENTICATED, Boolean.FALSE);
	}
}
