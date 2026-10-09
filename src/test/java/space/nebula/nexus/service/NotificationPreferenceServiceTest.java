package space.nebula.nexus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.entity.NotificationCategoryPreference;
import space.nebula.nexus.entity.NotificationPreference;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.NotificationCategory;
import space.nebula.nexus.payload.request.NotificationCategoryPreferenceRequest;
import space.nebula.nexus.payload.response.NotificationCategoryPreferenceResponse;
import space.nebula.nexus.repository.NotificationCategoryPreferenceRepository;
import space.nebula.nexus.repository.NotificationPreferenceRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {
	@Mock
	private NotificationCategoryPreferenceRepository categoryRepository;
	@Mock
	private NotificationPreferenceRepository preferenceRepository;
	@Mock
	private UserRepository userRepository;
	@InjectMocks
	private NotificationPreferenceService service;

	@Test
	void missingCategoryInheritsLegacySystemSwitch() {
		NotificationPreference base = new NotificationPreference();
		base.setSystemEnabled(false);
		base.setSystemEmailEnabled(true);
		base.setCommentEnabled(true);
		when(preferenceRepository.findByUserIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(base));
		when(categoryRepository.findByUserIdAndCategory(4L, NotificationCategory.CREATOR)).thenReturn(Optional.empty());
		when(categoryRepository.findByUserIdAndCategory(4L, NotificationCategory.COMMENT))
				.thenReturn(Optional.of(override(NotificationCategory.COMMENT, false, true)));

		assertEquals(new NotificationPreferenceService.Channels(false, true), service.resolve(4L, "POST_PUBLISHED"));
		assertEquals(new NotificationPreferenceService.Channels(false, true), service.resolve(4L, "COMMENT_REPLY"));
	}

	@Test
	void replacementClearsOmittedCategoriesAndStoresExplicitOverrides() {
		User user = new User();
		user.setId(4L);
		when(userRepository.findNotificationPreferenceOwnerForUpdate(4L)).thenReturn(Optional.of(user));
		NotificationCategoryPreference existing = override(NotificationCategory.REPORT, true, false);
		when(categoryRepository.findByUserId(4L)).thenReturn(List.of(existing), List.of());
		Map<NotificationCategory, NotificationCategoryPreferenceRequest.Channels> overrides = new EnumMap<>(
				NotificationCategory.class);
		overrides.put(NotificationCategory.MODERATION, new NotificationCategoryPreferenceRequest.Channels(true, true));

		try (MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
			security.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(user);
			var response = service.replaceMyCategories(new NotificationCategoryPreferenceRequest(overrides));
			assertTrue(response.categories().get(NotificationCategory.REPORT).inherited());
		}

		verify(categoryRepository).delete(existing);
		ArgumentCaptor<NotificationCategoryPreference> saved = ArgumentCaptor
				.forClass(NotificationCategoryPreference.class);
		verify(categoryRepository).save(saved.capture());
		assertEquals(NotificationCategory.MODERATION, saved.getValue().getCategory());
		assertEquals(4L, saved.getValue().getUserId());
		assertTrue(saved.getValue().getEmailEnabled());
	}

	@Test
	void responseMarksInheritedAndExplicitChannels() {
		User user = new User();
		user.setId(4L);
		NotificationPreference base = new NotificationPreference();
		base.setCommentEnabled(false);
		base.setCommentEmailEnabled(false);
		when(preferenceRepository.findByUserIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(base));
		when(categoryRepository.findByUserId(4L))
				.thenReturn(List.of(override(NotificationCategory.OPERATIONS, false, true)));

		try (MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
			security.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(user);
			var response = service.getMyCategories();
			NotificationCategoryPreferenceResponse.Channels comments = response.categories()
					.get(NotificationCategory.COMMENT);
			NotificationCategoryPreferenceResponse.Channels operations = response.categories()
					.get(NotificationCategory.OPERATIONS);
			assertTrue(comments.inherited());
			assertFalse(comments.inAppEnabled());
			assertFalse(operations.inherited());
			assertTrue(operations.emailEnabled());
		}
	}

	private NotificationCategoryPreference override(NotificationCategory category, boolean inApp, boolean email) {
		NotificationCategoryPreference row = new NotificationCategoryPreference();
		row.setUserId(4L);
		row.setCategory(category);
		row.setInAppEnabled(inApp);
		row.setEmailEnabled(email);
		return row;
	}
}
