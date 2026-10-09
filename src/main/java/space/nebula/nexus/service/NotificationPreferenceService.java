package space.nebula.nexus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.NotificationPreference;
import space.nebula.nexus.entity.NotificationCategoryPreference;
import space.nebula.nexus.enums.NotificationCategory;
import space.nebula.nexus.repository.NotificationPreferenceRepository;
import space.nebula.nexus.repository.NotificationCategoryPreferenceRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.payload.request.NotificationCategoryPreferenceRequest;
import space.nebula.nexus.payload.response.NotificationCategoryPreferenceResponse;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {
	private final NotificationCategoryPreferenceRepository categoryRepository;
	private final NotificationPreferenceRepository preferenceRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public Channels resolve(Long userId, String type) {
		NotificationCategory category = NotificationCategory.forType(type);
		return categoryRepository.findByUserIdAndCategory(userId, category)
				.map(row -> new Channels(row.getInAppEnabled(), row.getEmailEnabled()))
				.orElseGet(() -> inherited(preferenceRepository.findByUserIdAndIsDeletedFalse(userId).orElse(null),
						category));
	}

	@Transactional(readOnly = true)
	public NotificationCategoryPreferenceResponse getMyCategories() {
		return response(SecurityUtil.getCurrentUserOrThrow(userRepository).getId());
	}

	@Transactional
	public NotificationCategoryPreferenceResponse replaceMyCategories(NotificationCategoryPreferenceRequest request) {
		Long userId = SecurityUtil.getCurrentUserOrThrow(userRepository).getId();
		// Serialize complete replacements even when this user has no preference rows
		// yet.
		userRepository.findNotificationPreferenceOwnerForUpdate(userId).orElseThrow();
		Map<NotificationCategory, NotificationCategoryPreference> existing = new EnumMap<>(NotificationCategory.class);
		categoryRepository.findByUserId(userId).forEach(row -> existing.put(row.getCategory(), row));
		for (NotificationCategory category : NotificationCategory.values()) {
			var channels = request.overrides().get(category);
			var row = existing.get(category);
			if (channels == null) {
				if (row != null)
					categoryRepository.delete(row);
			} else {
				if (row == null) {
					row = new NotificationCategoryPreference();
					row.setUserId(userId);
					row.setCategory(category);
				}
				row.setInAppEnabled(channels.inAppEnabled());
				row.setEmailEnabled(channels.emailEnabled());
				categoryRepository.save(row);
			}
		}
		categoryRepository.flush();
		return response(userId);
	}

	private NotificationCategoryPreferenceResponse response(Long userId) {
		var base = preferenceRepository.findByUserIdAndIsDeletedFalse(userId).orElse(null);
		Map<NotificationCategory, NotificationCategoryPreferenceResponse.Channels> result = new EnumMap<>(
				NotificationCategory.class);
		for (var category : NotificationCategory.values()) {
			Channels channels = inherited(base, category);
			result.put(category,
					new NotificationCategoryPreferenceResponse.Channels(channels.inApp(), channels.email(), true));
		}
		categoryRepository.findByUserId(userId)
				.forEach(row -> result.put(row.getCategory(), new NotificationCategoryPreferenceResponse.Channels(
						row.getInAppEnabled(), row.getEmailEnabled(), false)));
		return new NotificationCategoryPreferenceResponse(result);
	}

	public static Channels inherited(NotificationPreference base, NotificationCategory category) {
		if (base == null)
			return new Channels(true, false);
		return switch (category) {
			case COMMENT -> new Channels(Boolean.TRUE.equals(base.getCommentEnabled()),
					Boolean.TRUE.equals(base.getCommentEmailEnabled()));
			case CATEGORY_POST -> new Channels(Boolean.TRUE.equals(base.getCategoryPostEnabled()),
					Boolean.TRUE.equals(base.getCategoryPostEmailEnabled()));
			default -> new Channels(Boolean.TRUE.equals(base.getSystemEnabled()),
					Boolean.TRUE.equals(base.getSystemEmailEnabled()));
		};
	}

	public record Channels(boolean inApp, boolean email) {
	}
}
