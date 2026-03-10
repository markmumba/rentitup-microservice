package com.rentitup.bff.config;

import com.google.protobuf.util.JsonFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.protobuf.ProtobufJsonFormatHttpMessageConverter;

@Configuration
public class ProtoJsonConfig {

	@Bean
	public ProtobufJsonFormatHttpMessageConverter protobufJsonConverter() {
		return new ProtobufJsonFormatHttpMessageConverter(
				JsonFormat.parser().ignoringUnknownFields(),
				JsonFormat.printer().omittingInsignificantWhitespace()
		);
	}
}
