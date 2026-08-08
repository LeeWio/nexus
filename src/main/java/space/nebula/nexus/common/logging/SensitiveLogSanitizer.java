package space.nebula.nexus.common.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Converts values into log-safe JSON without mutating the original request or
 * response objects.
 */
@Component
public class SensitiveLogSanitizer {

	private static final String MASK = "******";

	private static final String OMITTED = "[omitted]";

	private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of("password", "passwd", "secret", "token",
			"authorization", "credential", "otp", "code", "apikey", "accesskey", "privatekey", "cookie");

	private final ObjectMapper objectMapper = new ObjectMapper();

	/** Serializes method arguments after recursively masking sensitive fields. */
	public String sanitizeArguments(MethodSignature signature, Object[] args) {
		if (signature == null || args == null || args.length == 0) {
			return "{}";
		}

		String[] parameterNames = signature.getParameterNames();
		ObjectNode parameters = JsonNodeFactory.instance.objectNode();
		for (int index = 0; index < args.length; index++) {
			String name = parameterNames != null && index < parameterNames.length
					? parameterNames[index]
					: "arg" + index;
			parameters.set(name, sanitizeValue(name, args[index]));
		}
		return writeSafely(parameters);
	}

	/**
	 * Serializes an arbitrary result after recursively masking sensitive fields.
	 */
	public String sanitize(Object value) {
		return writeSafely(sanitizeValue(null, value));
	}

	/**
	 * Re-sanitizes JSON that was serialized by an earlier application version
	 * before it is persisted.
	 */
	public String sanitizeSerializedJson(String serializedValue) {
		if (serializedValue == null || serializedValue.isBlank()) {
			return serializedValue;
		}

		try {
			JsonNode node = objectMapper.readTree(serializedValue);
			if (node == null || node.isValueNode()) {
				return OMITTED;
			}
			redactNode(node);
			return writeSafely(node);
		} catch (Exception exception) {
			return OMITTED;
		}
	}

	private JsonNode sanitizeValue(String fieldName, Object value) {
		if (isSensitiveField(fieldName)) {
			return TextNode.valueOf(MASK);
		}
		if (value == null) {
			return JsonNodeFactory.instance.nullNode();
		}
		if (isUnsafeToLog(value)) {
			return TextNode.valueOf(OMITTED);
		}

		try {
			JsonNode node = objectMapper.valueToTree(value);
			redactNode(node);
			return node;
		} catch (IllegalArgumentException exception) {
			return TextNode.valueOf(OMITTED);
		}
	}

	private void redactNode(JsonNode node) {
		if (node instanceof ObjectNode objectNode) {
			boolean sensitiveConfigValue = hasSensitiveConfigKey(objectNode);
			objectNode.fields().forEachRemaining(entry -> {
				if (isSensitiveField(entry.getKey()) || (sensitiveConfigValue && isConfigValue(entry.getKey()))) {
					objectNode.put(entry.getKey(), MASK);
				} else {
					redactNode(entry.getValue());
				}
			});
			return;
		}
		if (node instanceof ArrayNode arrayNode) {
			arrayNode.forEach(this::redactNode);
		}
	}

	private boolean hasSensitiveConfigKey(ObjectNode objectNode) {
		return objectNode.properties().stream().filter(entry -> isConfigKey(entry.getKey()))
				.map(java.util.Map.Entry::getValue)
				.anyMatch(configKey -> configKey.isTextual() && isSensitiveField(configKey.asText()));
	}

	private boolean isConfigKey(String fieldName) {
		return "configkey".equals(normalizeFieldName(fieldName));
	}

	private boolean isConfigValue(String fieldName) {
		return "configvalue".equals(normalizeFieldName(fieldName));
	}

	private boolean isSensitiveField(String fieldName) {
		if (fieldName == null || fieldName.isBlank()) {
			return false;
		}
		String normalized = normalizeFieldName(fieldName);
		return SENSITIVE_FIELD_NAMES.stream().anyMatch(normalized::contains);
	}

	private String normalizeFieldName(String fieldName) {
		return fieldName.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
	}

	private boolean isUnsafeToLog(Object value) {
		return value instanceof MultipartFile || value instanceof InputStream || value instanceof ServletRequest
				|| value instanceof ServletResponse || value instanceof Resource || value instanceof byte[];
	}

	private String writeSafely(JsonNode node) {
		try {
			return objectMapper.writeValueAsString(node);
		} catch (Exception exception) {
			return "\"" + OMITTED + "\"";
		}
	}
}
