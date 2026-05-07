package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.payload.response.RoleResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponse toResponse(Role role);
    List<RoleResponse> toResponseList(List<Role> roles);
}
