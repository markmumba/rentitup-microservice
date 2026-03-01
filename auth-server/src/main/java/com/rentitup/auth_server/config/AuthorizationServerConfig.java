package com.rentitup.auth_server.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

	@Value("${auth.redirect-uri}")
	private String redirectUri;

	@Value("${auth.external-url}")
	private String externalUrl;

	/**
	 * For later reading and understanding :
	 * This security filter chain handles all the OAuth2/OIDC protocol endpoints.
	 * These endpoints include:
	 * - /oauth2/authorize (where users get redirected to log in)
	 * - /oauth2/token (where clients exchange codes for tokens)
	 * - /oauth2/jwks (where the public keys are published)
	 * - /oauth2/revoke (to invalidate tokens)
	 * - /oauth2/introspect (to check if a token is valid)
	 * - /.well-known/openid-configuration (discovery document)
	 */

	@Bean
	@Order(1)
	public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
				new OAuth2AuthorizationServerConfigurer();

		http
				.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
				.with(authorizationServerConfigurer, authServer ->
						authServer.oidc(Customizer.withDefaults())
				)
				.authorizeHttpRequests(authorize ->
						authorize.anyRequest().authenticated()
				)
				.exceptionHandling(exceptions ->
						exceptions.defaultAuthenticationEntryPointFor(
								new LoginUrlAuthenticationEntryPoint("/login"),
								new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
						)
				);

		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/login", "/error").permitAll()
						.anyRequest().authenticated()
				).formLogin(Customizer.withDefaults());
		return http.build();
	}


	/**
	 * The  RegisteredClientRepository stores information about all the applications
	 * (clients) that are allowed to use this Authorization Server.
	 * Think of this as a database of "approved apps." Each app has:
	 * - A client_id (public identifier)
	 * - A client_secret (private password, for confidential clients)
	 * - Allowed grant types (how they can get tokens)
	 * - Allowed scopes (what permissions they can request)
	 * - Redirect URIs (where users can be sent after login)
	 */

	@Bean
	public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {

		RegisteredClient bff = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("bff-gateway")
				.clientSecret(passwordEncoder.encode("bff-gateway-secret"))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.redirectUri(redirectUri)
				.scope("openid")
				.scope("profile")
				.scope("read")
				.scope("write")
				.tokenSettings(TokenSettings.builder()
						.accessTokenTimeToLive(Duration.ofHours(1))
						.refreshTokenTimeToLive(Duration.ofDays(7))
						.build())
				.clientSettings(ClientSettings.builder()
						.requireAuthorizationConsent(false)
						.build())

				.build();
		RegisteredClient bffService = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("bff-service")
				.clientSecret(passwordEncoder.encode("bff-secret"))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("internal")
				.scope("user:read")
				.scope("user:write")
				.scope("catalog:read")
				.scope("booking:read")
				.scope("booking:write")
				.tokenSettings(TokenSettings.builder()
						.accessTokenTimeToLive(Duration.ofMinutes(30))
						.build())
				.build();

		RegisteredClient catalogService = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("catalog-service")
				.clientSecret(passwordEncoder.encode("catalog-secret"))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("internal")
				.scope("user:read")
				.scope("machine:read")
				.scope("machine:write")
				.tokenSettings(TokenSettings.builder()
						.accessTokenTimeToLive(Duration.ofMinutes(30))
						.build())
				.build();

		RegisteredClient userService = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("user-service")
				.clientSecret(passwordEncoder.encode("user-secret"))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("internal")
				.scope("user:read")
				.scope("user:write")
				.tokenSettings(TokenSettings.builder()
						.accessTokenTimeToLive(Duration.ofMinutes(30))
						.build())
				.build();

		RegisteredClient bookingService = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("booking-service")
				.clientSecret(passwordEncoder.encode("booking-secret"))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("internal")
				.scope("user:read")
				.scope("catalog:read")
				.scope("booking:read")
				.scope("booking:write")
				.tokenSettings(TokenSettings.builder()
						.accessTokenTimeToLive(Duration.ofMinutes(30))
						.build())
				.build();

		return new InMemoryRegisteredClientRepository(
				bff,
				bffService,
				catalogService,
				userService,
				bookingService
		);
	}

	/**
	 JWKSource provides the cryptographic keys used to sign and verify JWTs.
	 * This is where the "asymmetric key" magic happens:
	 * - We generate an RSA key pair (private + public key)
	 * - The private key is used to SIGN tokens (only the Auth Server has this)
	 * - The public key is PUBLISHED via the /oauth2/jwks endpoint
	 * - Other services use the public key to VERIFY tokens
	 *
	 */
	@Bean
	public JWKSource<SecurityContext> jwkSource() {
		KeyPair keyPair = generateRsaKey();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

		RSAKey  rsaKey = new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(UUID.randomUUID().toString())
				.build();

		JWKSet jwkSet = new JWKSet(rsaKey);
		return  new ImmutableJWKSet<>(jwkSet);
	}

	private static KeyPair generateRsaKey() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			return keyPairGenerator.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Failed to generate RSA key", e);
		}
	}

	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		JWSKeySelector<SecurityContext> jwsKeySelector =
				new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
		ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
		jwtProcessor.setJWSKeySelector(jwsKeySelector);
		return new NimbusJwtDecoder(jwtProcessor);
	}

	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder()
				.issuer(externalUrl)
				.build();
	}


	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}




