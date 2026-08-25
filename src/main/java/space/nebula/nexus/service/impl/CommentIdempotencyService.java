package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentIdempotencyService {

	private final JdbcTemplate jdbcTemplate;

	public String hashSubmission(Long postId, Long parentId, String content) {
		String value = (postId == null ? "" : postId) + "|" + (parentId == null ? "" : parentId) + "|"
				+ (content == null ? "" : content);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 digest is not available", ex);
		}
	}

	public Optional<ApiResponse<Void>> begin(Long userId, String idempotencyKey, String requestHash) {
		if (idempotencyKey == null) {
			return Optional.empty();
		}
		int inserted = jdbcTemplate.update(
				"INSERT IGNORE INTO blog_comment_idempotency(user_id, idempotency_key, request_hash, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
				userId, idempotencyKey, requestHash);
		if (inserted > 0) {
			return Optional.empty();
		}

		IdempotencyRecord existing = find(userId, idempotencyKey).orElseThrow(
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Idempotency-Key is already being processed"));
		if (!existing.requestHash().equals(requestHash)) {
			throw new BusinessException(BusinessCode.DUPLICATE_KEY,
					"Idempotency-Key was already used for a different comment");
		}
		if (existing.responseCode() == null) {
			return Optional.of(ApiResponse.success("Comment submission already received.", null));
		}
		return Optional.of(ApiResponse.success(existing.responseMessage(), null));
	}

	public void complete(Long userId, String idempotencyKey, String requestHash, ApiResponse<?> response,
			Long commentId) {
		if (idempotencyKey == null) {
			return;
		}
		jdbcTemplate.update(
				"UPDATE blog_comment_idempotency SET response_code = ?, response_message = ?, comment_id = ?, completed_at = CURRENT_TIMESTAMP WHERE user_id = ? AND idempotency_key = ? AND request_hash = ?",
				response.code(), response.message(), commentId, userId, idempotencyKey, requestHash);
	}

	public Optional<Long> findCompletedCommentId(Long userId, String idempotencyKey, String requestHash) {
		return find(userId, idempotencyKey).filter(record -> record.requestHash().equals(requestHash))
				.map(IdempotencyRecord::commentId);
	}

	private Optional<IdempotencyRecord> find(Long userId, String idempotencyKey) {
		return jdbcTemplate.query("""
				SELECT request_hash, response_code, response_message, comment_id
				FROM blog_comment_idempotency
				WHERE user_id = ? AND idempotency_key = ?
				""", rs -> {
			if (!rs.next()) {
				return Optional.empty();
			}
			Long commentId = rs.getLong("comment_id");
			if (rs.wasNull()) {
				commentId = null;
			}
			Integer responseCode = rs.getObject("response_code", Integer.class);
			return Optional.of(new IdempotencyRecord(rs.getString("request_hash"), responseCode,
					rs.getString("response_message"), commentId));
		}, userId, idempotencyKey);
	}

	private record IdempotencyRecord(String requestHash, Integer responseCode, String responseMessage, Long commentId) {
	}
}
