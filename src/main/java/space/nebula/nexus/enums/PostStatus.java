package space.nebula.nexus.enums;

import lombok.Getter;

@Getter
public enum PostStatus {
	DRAFT("Draft"), PUBLISHED("Published"), ARCHIVED("Archived");

	private final String description;

	PostStatus(String description) {
		this.description = description;
	}
}
