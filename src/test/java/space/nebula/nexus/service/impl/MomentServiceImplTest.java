package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.FileMetadata;
import space.nebula.nexus.entity.Moment;
import space.nebula.nexus.entity.MomentTopic;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.mapper.MomentMapper;
import space.nebula.nexus.payload.request.MomentImageRequest;
import space.nebula.nexus.payload.request.MomentRequest;
import space.nebula.nexus.repository.FileRepository;
import space.nebula.nexus.repository.MomentRepository;
import space.nebula.nexus.repository.MomentTopicRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.enums.MomentVisibility;
import space.nebula.nexus.security.util.SecurityUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomentServiceImplTest {

	@Mock
	private MomentRepository momentRepository;
	@Mock
	private MomentTopicRepository momentTopicRepository;
	@Mock
	private MomentMapper momentMapper;
	@Mock
	private FileRepository fileRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private JdbcTemplate jdbcTemplate;

	private MomentServiceImpl momentService;
	private User user;

	@BeforeEach
	void setUp() {
		momentService = new MomentServiceImpl(momentRepository, momentTopicRepository, momentMapper, fileRepository, userRepository,
				jdbcTemplate);
		user = new User();
		user.setId(4L);
		user.setUsername("reader");
	}

	@Test
	void createMomentAttachesValidatedImagesInRequestOrder() {
		Moment moment = publishedMoment(null);
		MomentRequest request = new MomentRequest("A small field note", MomentVisibility.PUBLIC,
				List.of(new MomentImageRequest(12L, "Second view"), new MomentImageRequest(11L, "First view")), List.of());
		FileMetadata first = image(11L, "first.jpg", "image/jpeg");
		FileMetadata second = image(12L, "second.webp", "image/webp");
		when(momentMapper.toEntity(request)).thenReturn(moment);
		when(fileRepository.findAllById(anyCollection())).thenReturn(List.of(first, second));

		momentService.createMoment(request);

		assertEquals(2, moment.getImages().size());
		assertSame(second, moment.getImages().get(0).getFile());
		assertEquals(0, moment.getImages().get(0).getSortOrder());
		assertEquals("Second view", moment.getImages().get(0).getAltText());
		assertSame(first, moment.getImages().get(1).getFile());
		verify(momentRepository).save(moment);
	}

	@Test
	void createMomentAllowsAnImageWithoutText() {
		Moment moment = publishedMoment(null);
		MomentRequest request = new MomentRequest("", MomentVisibility.PUBLIC,
				List.of(new MomentImageRequest(12L, "A field note photo")), List.of());
		when(momentMapper.toEntity(request)).thenReturn(moment);
		when(fileRepository.findAllById(anyCollection())).thenReturn(List.of(image(12L, "field-note.jpg", "image/jpeg")));

		momentService.createMoment(request);

		verify(momentRepository).save(moment);
	}

	@Test
	void createMomentRejectsAnEmptyTextOnlyRequest() {
		MomentRequest request = new MomentRequest("", MomentVisibility.PUBLIC, List.of(), List.of());

		assertThrows(BusinessException.class, () -> momentService.createMoment(request));
		verifyNoInteractions(momentMapper, fileRepository, momentRepository);
	}

	@Test
	void createMomentRejectsVisibleTextOverTheComposerLimit() {
		MomentRequest request = new MomentRequest("a".repeat(2001), MomentVisibility.PUBLIC, List.of(), List.of());

		assertThrows(BusinessException.class, () -> momentService.createMoment(request));
		verifyNoInteractions(momentMapper, fileRepository, momentRepository);
	}

	@Test
	void createMomentRejectsNonImageAssets() {
		Moment moment = publishedMoment(null);
		MomentRequest request = new MomentRequest("Attachment", MomentVisibility.PUBLIC,
				List.of(new MomentImageRequest(13L, "A document")), List.of());
		when(momentMapper.toEntity(request)).thenReturn(moment);
		when(fileRepository.findAllById(anyCollection()))
				.thenReturn(List.of(image(13L, "notes.pdf", "application/pdf")));

		assertThrows(BusinessException.class, () -> momentService.createMoment(request));
		verifyNoInteractions(momentRepository);
	}

	@Test
	void createMomentNormalizesTopicsAndPreservesTheirSelectionOrder() {
		Moment moment = publishedMoment(null);
		MomentTopic existingTopic = topic(8L, "frontend-architecture");
		MomentRequest request = new MomentRequest("A small field note", MomentVisibility.PUBLIC, List.of(),
				List.of("#Frontend Architecture", "Observability"));
		when(momentMapper.toEntity(request)).thenReturn(moment);
		when(momentTopicRepository.findBySlug("frontend-architecture")).thenReturn(Optional.of(existingTopic));
		when(momentTopicRepository.findBySlug("observability")).thenReturn(Optional.empty());
		when(momentTopicRepository.save(any(MomentTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));

		momentService.createMoment(request);

		assertEquals(2, moment.getTopicRelations().size());
		assertEquals("frontend-architecture", moment.getTopicRelations().get(0).getTopic().getSlug());
		assertEquals(0, moment.getTopicRelations().get(0).getSortOrder());
		assertEquals("observability", moment.getTopicRelations().get(1).getTopic().getSlug());
		assertEquals(1, moment.getTopicRelations().get(1).getSortOrder());
	}

	@Test
	void duplicateMomentLikeOnlyIncrementsCounterOnce() {
		Moment moment = publishedMoment(7L);
		when(momentRepository.findById(7L)).thenReturn(Optional.of(moment));
		when(jdbcTemplate.update(anyString(), eq(7L), eq(4L))).thenReturn(1, 0);

		withAuthenticatedUser(() -> {
			momentService.likeMoment(7L);
			momentService.likeMoment(7L);
		});

		verify(momentRepository).incrementLikes(7L, 1L);
	}

	@Test
	void duplicateMomentUnlikeOnlyDecrementsCounterOnce() {
		Moment moment = publishedMoment(7L);
		when(momentRepository.findById(7L)).thenReturn(Optional.of(moment));
		when(jdbcTemplate.update(anyString(), eq(7L), eq(4L))).thenReturn(1, 0);

		withAuthenticatedUser(() -> {
			momentService.unlikeMoment(7L);
			momentService.unlikeMoment(7L);
		});

		verify(momentRepository).incrementLikes(7L, -1L);
	}

	@Test
	void likeMomentRejectsInvisibleMoment() {
		Moment moment = new Moment();
		moment.setId(7L);
		moment.setVisibility(MomentVisibility.PRIVATE);
		moment.setCreatedBy("another_user");
		when(momentRepository.findById(7L)).thenReturn(Optional.of(moment));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(user);
			mockedSecurity.when(SecurityUtil::getCurrentUsername).thenReturn("reader");

			assertThrows(BusinessException.class, () -> momentService.likeMoment(7L));
		}

		verifyNoInteractions(jdbcTemplate);
	}

	@Test
	void getLikedMomentIdsReturnsOnlyCurrentUsersLikes() {
		when(momentRepository.findLikedMomentIdsByUserIdAndMomentIdIn(eq(4L), anyCollection())).thenReturn(List.of(9L));

		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(user);

			var response = momentService.getLikedMomentIds(List.of(7L, 9L, 7L));

			assertEquals(Set.of(9L), response.data());
		}
	}

	private void withAuthenticatedUser(Runnable action) {
		try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
			mockedSecurity.when(() -> SecurityUtil.getCurrentUserOrThrow(userRepository)).thenReturn(user);
			action.run();
		}
	}

	private static Moment publishedMoment(Long id) {
		Moment moment = new Moment();
		moment.setId(id);
		moment.setVisibility(MomentVisibility.PUBLIC);
		return moment;
	}

	private static FileMetadata image(Long id, String name, String type) {
		FileMetadata file = new FileMetadata();
		file.setId(id);
		file.setFileName(name);
		file.setFileType(type);
		file.setIsDeleted(false);
		return file;
	}

	private static MomentTopic topic(Long id, String slug) {
		MomentTopic topic = new MomentTopic();
		topic.setId(id);
		topic.setSlug(slug);
		return topic;
	}
}
