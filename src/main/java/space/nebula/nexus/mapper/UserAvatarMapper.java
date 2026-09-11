package space.nebula.nexus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.utils.UserAvatars;

/**
 * Maps a user entity to a display avatar URL.
 */
@Mapper(componentModel = "spring")
public interface UserAvatarMapper {

	/**
	 * Resolves a stored, GitHub, or email-based avatar for API responses.
	 *
	 * @param user
	 *            account whose avatar should be displayed
	 * @return avatar URL, or {@code null} when no source is available
	 */
	@Named("resolveUserAvatar")
	default String resolveUserAvatar(User user) {
		if (user == null) {
			return null;
		}
		return UserAvatars.resolve(user.getAvatar(), user.getEmail(), user.getGithubUsername());
	}
}
