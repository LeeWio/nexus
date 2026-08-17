package space.nebula.nexus.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Represents the visibility settings of a micro-blog 'Moment'.
 */
@Getter
public enum MomentVisibility {
	/**
	 * The moment is visible to everyone.
	 */
	PUBLIC("public"),

	/**
	 * Only followers can view the moment.
	 */
	FOLLOWERS("followers"),

	/**
	 * Only the creator can view the moment.
	 */
	PRIVATE("private");

	private final String value;

	MomentVisibility(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
