package space.nebula.nexus.service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.PostEditorDraft;
import space.nebula.nexus.repository.PostEditorDraftRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostEditorDraftService {
	public static final String NEW_ARTICLE = "new";
	public static final String IDENTIFIER = "(?:new|[1-9][0-9]{0,18})";

	private final PostEditorDraftRepository drafts;
	private final UserRepository users;

	@Transactional(readOnly = true)
	public Snapshot retrieve(String identifier) {
		return drafts.findByUserIdAndIdentifier(currentUserId(), identifier).map(Snapshot::from).orElse(null);
	}

	@Transactional
	public Snapshot save(String identifier, SaveDraft request) {
		Long userId = currentUserId();
		lockOwner(userId);
		PostEditorDraft draft = drafts.findForUpdate(userId, identifier).orElse(null);
		if (draft != null && draft.getContent().equals(request.content())
				&& draft.getPostData().equals(request.postData())) {
			return Snapshot.from(draft);
		}
		verifyRevision(draft, request.revision());
		if (draft == null) {
			draft = new PostEditorDraft();
			draft.setUserId(userId);
			draft.setIdentifier(identifier);
		}
		draft.setContent(request.content());
		draft.setPostData(request.postData());
		draft.setSavedAt(LocalDateTime.now(ZoneOffset.UTC));
		draft.setRevision(UUID.randomUUID().toString());
		return Snapshot.from(drafts.save(draft));
	}

	@Transactional
	public void discard(String identifier, String revision) {
		Long userId = currentUserId();
		lockOwner(userId);
		PostEditorDraft draft = drafts.findForUpdate(userId, identifier).orElse(null);
		if (draft != null) {
			verifyRevision(draft, revision);
			drafts.delete(draft);
		}
	}

	/**
	 * Consume the snapshot alongside the article save. Existing API clients may
	 * omit the token.
	 */
	@Transactional
	public void consume(Long userId, String identifier, String revision) {
		if (revision == null)
			return;
		lockOwner(userId);
		PostEditorDraft draft = drafts.findForUpdate(userId, identifier).orElse(null);
		verifyRevision(draft, "none".equals(revision) ? null : revision);
		if (draft != null)
			drafts.delete(draft);
	}

	private void lockOwner(Long userId) {
		// The owner row also serializes creation before the draft row exists.
		users.findByIdForUpdate(userId).orElseThrow();
	}

	private Long currentUserId() {
		return SecurityUtil.getCurrentUserOrThrow(users).getId();
	}

	private static void verifyRevision(PostEditorDraft draft, String revision) {
		if (!Objects.equals(draft == null ? null : draft.getRevision(), revision)) {
			throw new OptimisticLockingFailureException("Editor draft changed");
		}
	}

	public record SaveDraft(@NotNull Map<String, Object> content, @NotNull Map<String, Object> postData,
			@Size(max = 36) String revision) {
	}

	public record Snapshot(Object content, Map<String, Object> postData, String revision, Instant savedAt) {
		private static Snapshot from(PostEditorDraft draft) {
			return new Snapshot(draft.getContent(), draft.getPostData(), draft.getRevision(),
					draft.getSavedAt().toInstant(ZoneOffset.UTC));
		}
	}
}
