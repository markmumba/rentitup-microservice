package com.rentitup.catalog_service.interceptor;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.util.Set;


@GlobalServerInterceptor
@Order(100)
@RequiredArgsConstructor
@Service
@Slf4j
public class JwtAuthInterceptor implements ServerInterceptor {

	private final JwtDecoder jwtDecoder;


	public static final Context.Key<String> USER_ID = Context.key("user-id");
	public static final Context.Key<String> USER_TYPE = Context.key("user-type");
	public static final Context.Key<String> USER_EMAIL = Context.key("user-email");
	public static final Context.Key<String> TOKEN_TYPE = Context.key("token-type");
	public static final Context.Key<Boolean> IS_AUTHENTICATED = Context.key("is-authenticated");
	public static final Context.Key<Set<String>> SCOPES = Context.key("scopes");

	private static final Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

	private static final Set<String> PUBLIC_METHODS = Set.of(
			"rentitup.catalog.CatalogService/ListCategories",
			"rentitup.catalog.CatalogService/GetCategory",
			"rentitup.catalog.CatalogService/ListMachines",
			"rentitup.catalog.CatalogService/GetMachine",
			"rentitup.catalog.CatalogService/SearchMachines",
			"grpc.health.v1.Health/Check"
	);



	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
		String methodName = call.getMethodDescriptor().getFullMethodName();
		boolean isPublicMethod = PUBLIC_METHODS.contains(methodName);

		log.debug("interceptor call to: {} (public: {})", methodName, isPublicMethod);

		String authHeader = headers.get(AUTHORIZATION_HEADER);
		Context context ;

		if(authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			try {
				Jwt jwt = jwtDecoder.decode(token);

				String userId = jwt.getClaimAsString("user_id");
				String userType = jwt.getClaimAsString("user_type");
				String email = jwt.getClaimAsString("email");
				String tokenType = jwt.getClaimAsString("token_type");

				log.debug("Valid token for user: {} {type: {})",email,tokenType);

				context = Context.current()
						.withValue(USER_ID, userId)
						.withValue(USER_TYPE, userType)
						.withValue(USER_EMAIL, email)
						.withValue(TOKEN_TYPE, tokenType)
				.withValue(IS_AUTHENTICATED, Boolean.TRUE);
			}catch (JwtException e) {
				log.warn("Invalid Jwt token: {}", e.getMessage());

				if (!isPublicMethod) {
					call.close(
							Status.UNAUTHENTICATED.withDescription("Invalid token: " + e.getMessage()),
							new Metadata()
					);
					return new ServerCall.Listener<ReqT>() {};
				}

				context = Context.current().withValue(IS_AUTHENTICATED, Boolean.FALSE);
			}

		}else {
			if( !isPublicMethod) {

				log.warn("No token provided for protected method: {}", methodName);
				call.close(
						Status.UNAUTHENTICATED.withDescription("Authentication required"),
						new Metadata()
				);
				return new ServerCall.Listener<>() {};
			}

			context = Context.current().withValue(IS_AUTHENTICATED, false);
		}

		return  Contexts.interceptCall(context, call, headers, next);
	}
}
