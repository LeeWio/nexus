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
public class PostValidator
{

	private final CategoryRepository categoryRepository;

	/**
	 * Validates the metadata and constraints for creating or updating a post.
	 */
	public void validatePostRequest(PostRequest request)
	{
		// 1. Content integrity
		Assert.notBlank(request.title(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post title is required"));
		Assert.notBlank(request.content(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post content is required"));

		// 2. Relation validation
		if (request.categoryId() != null)
		{
			Assert.isTrue(categoryRepository.existsById(request.categoryId()),
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "Selected category does not exist"));
		}
	}
}
