package space.nebula.nexus.payload.response;

import java.io.Serializable;

public record RoleResponse(Long id, String name, String code, String description) implements Serializable {
	private static final long serialVersionUID = 1L;
}
