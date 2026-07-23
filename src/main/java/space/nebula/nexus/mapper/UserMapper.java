package space.nebula.nexus.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import space.nebula.nexus.entity.Menu;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.response.UserResponse;
import space.nebula.nexus.payload.response.UserInfoResponse;
import space.nebula.nexus.mapper.config.CentralMapperConfig;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(config = CentralMapperConfig.class, uses = {RoleMapper.class})
public interface UserMapper {

	@Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoleCodes")
	UserResponse toResponse(User user);

	List<UserResponse> toResponseList(List<User> users);

	@Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoleCodes")
	@Mapping(target = "permissions", source = "roles", qualifiedByName = "mapPermissions")
	UserInfoResponse toInfoResponse(User user);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "username", ignore = true)
	@Mapping(target = "password", ignore = true)
	@Mapping(target = "tokenVersion", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "roles", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "lastModifiedBy", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "githubId", ignore = true)
	@Mapping(target = "googleId", ignore = true)
	@Mapping(target = "githubUsername", ignore = true)
	void updateEntity(@MappingTarget User user, space.nebula.nexus.payload.request.UserProfileRequest request);

	@Named("mapRoleCodes")
	default Set<String> mapRoleCodes(Set<Role> roles) {
		if (roles == null)
			return null;
		return roles.stream().map(Role::getCode).collect(Collectors.toSet());
	}

	@Named("mapPermissions")
	default Set<String> mapPermissions(Set<Role> roles) {
		if (roles == null)
			return null;
		return roles.stream().flatMap(role -> role.getMenus().stream()).map(Menu::getPermission)
				.filter(p -> p != null && !p.isBlank()).collect(Collectors.toSet());
	}
}
