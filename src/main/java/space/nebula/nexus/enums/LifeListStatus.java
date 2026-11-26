package space.nebula.nexus.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

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

	public static LifeListStatus fromQueryParameter(String source) {
		String status = source.trim();
		if (status.isEmpty()) {
			return null;
		}

		return Arrays.stream(values())
				.filter(candidate -> candidate.value.equals(status) || candidate.name().equals(status)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown life-list status"));
	}
}
