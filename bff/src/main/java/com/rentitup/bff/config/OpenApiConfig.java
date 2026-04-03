package com.rentitup.bff.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfig {

	@Value("${server.port:8080}")
	private String serverPort;

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("RentItUp API")
						.description("Backend for Frontend (BFF) API for RentItUp - Machine Rental Platform")
						.version("1.0.0")
						.contact(new Contact()
								.name("RentItUp Team")
								.email("support@rentitup.com"))
						.license(new License()
								.name("MIT License")
								.url("https://opensource.org/licenses/MIT")))
				.servers(List.of(
						new Server()
								.url("http://localhost:" + serverPort)
								.description("Local Development Server")
				));
	}

	@Bean
	public OpenApiCustomizer protoOpenApiCustomizer() {
		return openApi -> {
			if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
				openApi.getComponents().getSchemas().values().forEach(schema -> {
					if (schema.getProperties() != null) {
						@SuppressWarnings("unchecked")
						List<String> protoFields = ((Set<String>) schema.getProperties().keySet()).stream()
								.filter(key -> key.endsWith("Bytes")
										|| key.endsWith("OrBuilder")
										|| key.equals("unknownFields")
										|| key.equals("defaultInstanceForType")
										|| key.equals("parserForType")
										|| key.equals("serializedSize")
										|| key.equals("initialized")
										|| key.equals("allFields")
										|| key.equals("descriptorForType")
										|| key.equals("initializationErrorString")
										|| key.equals("memoizedSerializedSize"))
								.toList();
						protoFields.forEach(schema.getProperties()::remove);
					}
				});
			}
		};
	}
}
