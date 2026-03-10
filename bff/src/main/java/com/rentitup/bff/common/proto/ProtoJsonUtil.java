package com.rentitup.bff.common.proto;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Converts protobuf Messages to plain Java Maps so Jackson can serialize them
 * without needing a custom serializer. Uses snake_case field names (preservingProtoFieldNames).
 */
public final class ProtoJsonUtil {

	private static final JsonFormat.Printer PRINTER = JsonFormat.printer()
			.preservingProtoFieldNames()
			.omittingInsignificantWhitespace();

	private ProtoJsonUtil() {}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> toMap(Message message) {
		try {
			String json = PRINTER.print(message);
			return new ObjectMapper().readValue(json, Map.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert protobuf to map", e);
		}
	}

	public static List<Map<String, Object>> toMapList(List<? extends Message> messages) {
		return messages.stream().map(ProtoJsonUtil::toMap).toList();
	}
}
