package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import space.nebula.nexus.enums.MomentVisibility;

import java.util.List;

@Schema(description = "Request to create or update a micro-blog 'Moment'")
public record MomentRequest(
		@Schema(description = "Tiptap JSON or legacy plain text. Visible text is limited to 2,000 characters; an empty string is valid when images are attached.", example = "Just finished a marathon session of coding! #productive") @NotNull(message = "Content is required") @Size(max = 16000, message = "Serialized Moment content must not exceed 16000 characters") String content,

		@Schema(description = "The visibility level of this moment", example = "public") @NotNull(message = "Visibility status is required") MomentVisibility visibility,

		@Schema(description = "Ordered uploaded images. Omit or send an empty list for a text-only Moment.") @Size(max = 9, message = "A moment can contain at most 9 images") List<@Valid MomentImageRequest> images,

		@Schema(description = "One to three optional social topic slugs. Values are normalized and created on demand; omit on update to preserve existing topics.", example = "[\"frontend-architecture\", \"web-performance\"]") @Size(max = 3, message = "A moment can contain at most 3 topics") List<String> topicSlugs,

		@Schema(description = "Optional attached stock symbol", example = "AAPL") @Size(max = 20, message = "Stock symbol must not exceed 20 characters") String stockSymbol) {
}
