package space.nebula.nexus.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Applies the character semantics used by the Moment composer to persisted
 * rich-text payloads. The limit concerns visible text, not JSON markup.
 */
public final class MomentContentPolicy {

	/**
	 * The platform hard cap. The composer retains a 280-character short-form
	 * milestone, while allowing a full Moment to carry more context.
	 */
	public static final int MAX_VISIBLE_CHARACTERS = 2000;

	private static final String RICH_TEXT_SCHEMA_ID = "odyssey.rich-text";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private MomentContentPolicy() {
	}

	public static int visibleCharacterCount(String content) {
		return visibleText(content).length();
	}

	public static boolean hasVisibleText(String content) {
		return !visibleText(content).isBlank();
	}

	private static String visibleText(String content) {
		if (content == null || content.isEmpty()) {
			return "";
		}

		String trimmed = content.trim();
		if (!trimmed.startsWith("{")) {
			return content;
		}

		try {
			JsonNode document = asRichTextDocument(OBJECT_MAPPER.readTree(content));
			if (document == null) {
				return content;
			}

			StringBuilder text = new StringBuilder();
			appendText(document, text);
			return text.toString();
		} catch (Exception ignored) {
			// Legacy plain-text Moments may begin with a left brace.
			return content;
		}
	}

	private static JsonNode asRichTextDocument(JsonNode root) {
		if (isDocument(root)) {
			return root;
		}

		JsonNode document = root.path("document");
		return RICH_TEXT_SCHEMA_ID.equals(root.path("schema").asText()) && isDocument(document) ? document : null;
	}

	private static boolean isDocument(JsonNode node) {
		return node.isObject() && "doc".equals(node.path("type").asText()) && node.path("content").isArray();
	}

	private static void appendText(JsonNode node, StringBuilder text) {
		JsonNode value = node.get("text");
		if (value != null && value.isTextual()) {
			text.append(value.asText());
		}

		for (JsonNode child : node.path("content")) {
			appendText(child, text);
		}
	}
}
