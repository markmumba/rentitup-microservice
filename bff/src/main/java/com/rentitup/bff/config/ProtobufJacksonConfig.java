package com.rentitup.bff.config;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

@Configuration
public class ProtobufJacksonConfig {

	private static final JsonFormat.Printer PRINTER = JsonFormat.printer()
			.omittingInsignificantWhitespace()
			.preservingProtoFieldNames();

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public JsonMapperBuilderCustomizer protobufCustomizer() {
		return builder -> {
			SimpleModule module = new SimpleModule("ProtobufModule");
			module.addSerializer(Message.class, new ProtobufSerializer());
			builder.addModule(module);
		};
	}

	private static class ProtobufSerializer extends StdSerializer<Message> {

		public ProtobufSerializer() {
			super(Message.class);
		}

		@Override
		public void serialize(Message value, JsonGenerator gen, SerializationContext ctxt) {
			try {
				String json = PRINTER.print(value);
				gen.writeRawValue(json);
			} catch (Exception e) {
				throw new RuntimeException("Failed to serialize protobuf message", e);
			}
		}
	}
}
