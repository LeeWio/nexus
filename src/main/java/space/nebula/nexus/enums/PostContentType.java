package space.nebula.nexus.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Supported content formats for blog posts.
 */
@Schema(description = "Post content format")
public enum PostContentType
{
	@Schema(description = "Structured JSON format for block-based editors")
	JSON,

	@Schema(description = "Markdown with JSX support")
	MDX
}
