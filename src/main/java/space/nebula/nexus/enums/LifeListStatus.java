package space.nebula.nexus.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Current standing of a life-list wish. Completion is a state, not a score.
 */
@Getter
public enum LifeListStatus {
	PLANNED("planned"), IN_PROGRESS("in_progress"), COMPLETED("completed"), ABANDONED("abandoned");

	private final String value;

	LifeListStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
