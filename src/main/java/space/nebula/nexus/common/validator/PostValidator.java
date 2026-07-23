package space.nebula.nexus.common.validator;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.repository.CategoryRepository;

/**
 * Specialized validator for blog post business rules.
 */
@Component
@RequiredArgsConstructor
public class PostValidator {
	private static final int MAX_TITLE_LENGTH = 200;
	private static final int MAX_SUMMARY_LENGTH = 500;
	private static final int MAX_CONTENT_LENGTH = 1_000_000;

	private final CategoryRepository categoryRepository;

	/**
	 * Validates the metadata and constraints for creating or updating a post.
	 */
	public void validatePostRequest(PostRequest request) {
		// 1. Content integrity
		Assert.notBlank(request.title(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post title is required"));
		Assert.notBlank(request.content(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post content is required"));
		Assert.isTrue(request.title().trim().length() <= MAX_TITLE_LENGTH,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post title must not exceed 200 characters"));
		if (request.summary() != null) {
			Assert.isTrue(request.summary().trim().length() <= MAX_SUMMARY_LENGTH,
					() -> new BusinessException(BusinessCode.BAD_REQUEST,
							"Post summary must not exceed 500 characters"));
		}
		Assert.isTrue(request.content().length() <= MAX_CONTENT_LENGTH,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post content must not exceed 1 MB"));

		// 2. Relation validation
		if (request.categoryId() != null) {
			Assert.isTrue(categoryRepository.existsById(request.categoryId()),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "Selected category does not exist"));
		}
	}
}
