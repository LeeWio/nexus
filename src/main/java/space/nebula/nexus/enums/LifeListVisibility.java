package space.nebula.nexus.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Who may read a life-list item or memory. Public pages never return private rows.
 */
@Getter
public enum LifeListVisibility {
	PUBLIC("public"), PRIVATE("private");

	private final String value;

	LifeListVisibility(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
