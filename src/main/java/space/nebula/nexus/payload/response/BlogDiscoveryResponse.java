package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * Curated public blog content grouped for a discovery-oriented layout.
 */
@Schema(description = "Curated content for the public blog discovery experience")
public record BlogDiscoveryResponse(
		@Schema(description = "Primary editorial post") PostDigestResponse spotlight,
		@Schema(description = "Recently published posts") List<PostDigestResponse> latest,
		@Schema(description = "Most-read posts not already shown") List<PostDigestResponse> mostRead)
		implements Serializable
{
	private static final long serialVersionUID = 1L;

	public BlogDiscoveryResponse
	{
		latest = List.copyOf(latest);
		mostRead = List.copyOf(mostRead);
	}
}
