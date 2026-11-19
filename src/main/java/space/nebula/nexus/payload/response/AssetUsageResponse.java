package space.nebula.nexus.payload.response;

import java.util.List;

public record AssetUsageResponse(long total, List<Location> locations) {
	public record Location(Kind kind, Long id, String label) {
	}

	public enum Kind {
		ARTICLE_COVER, ARTICLE_CONTENT, ARTICLE_SUMMARY, COMMENT, USER_AVATAR, PROJECT_COVER, MOMENT, SERIES_COVER, FRIEND_LINK_AVATAR, ARTICLE_REVISION
	}
}
