package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.mapper.TagMapper;
import space.nebula.nexus.payload.request.TagRequest;
import space.nebula.nexus.payload.response.TagResponse;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.service.ITagService;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements ITagService
{

	private final TagRepository tagRepository;
	private final PostRepository postRepository;
	private final TagMapper tagMapper;
	private final RedisUtil redisUtil;

	@Override
	@Cacheable(value = CacheConstants.TAGS, key = "'all'", sync = true)
	public ApiResponse<List<TagResponse>> getAllTags()
	{
		return ApiResponse.success(tagMapper.toResponseList(tagRepository.findAll()));
	}

	@Override
	@Transactional
	@LogOperation("Create Tag")
	@CacheEvict(value = { CacheConstants.TAGS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<TagResponse> createTag(TagRequest request)
	{
		// Check name and slug including deleted rows for self-healing / seamless restore
		if (StrUtil.isNotBlank(request.name()))
		{
			var existingByName = tagRepository.findByNameIncludeDeleted(request.name());
			if (existingByName.isPresent())
			{
				Tag tag = existingByName.get();
				if (Boolean.TRUE.equals(tag.getIsDeleted()))
				{
					tag.setIsDeleted(false);
					tagMapper.updateEntity(tag, request);
					tagRepository.save(tag);
					log.info("Restored soft-deleted tag by name: {}", tag.getName());
					return ApiResponse.success("Tag created successfully", tagMapper.toResponse(tag));
				}
				else
				{
					throw new BusinessException(BusinessCode.DUPLICATE_KEY, "Tag name already exists: " + request.name());
				}
			}
		}

		if (StrUtil.isNotBlank(request.slug()))
		{
			var existingBySlug = tagRepository.findBySlugIncludeDeleted(request.slug());
			if (existingBySlug.isPresent())
			{
				Tag tag = existingBySlug.get();
				if (Boolean.TRUE.equals(tag.getIsDeleted()))
				{
					tag.setIsDeleted(false);
					tagMapper.updateEntity(tag, request);
					tagRepository.save(tag);
					log.info("Restored soft-deleted tag by slug: {}", tag.getName());
					return ApiResponse.success("Tag created successfully", tagMapper.toResponse(tag));
				}
				else
				{
					throw new BusinessException(BusinessCode.DUPLICATE_KEY, "Tag slug already exists: " + request.slug());
				}
			}
		}

		Tag tag = new Tag();
		tagMapper.updateEntity(tag, request);

		tagRepository.save(tag);
		log.info("Tag created: {}", tag.getName());
		return ApiResponse.success("Tag created successfully", tagMapper.toResponse(tag));
	}

	@Override
	@Transactional
	@LogOperation("Update Tag")
	@CacheEvict(value = { CacheConstants.TAGS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<TagResponse> updateTag(Long id, TagRequest request)
	{
		Tag existingTag = tagRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

		validateUniqueConstraints(existingTag, request);

		tagMapper.updateEntity(existingTag, request);
		tagRepository.save(existingTag);

		log.info("Tag updated: {}", existingTag.getName());
		return ApiResponse.success("Tag updated successfully", tagMapper.toResponse(existingTag));
	}

	@Override
	@Transactional
	@LogOperation("Delete Tag")
	@CacheEvict(value = { CacheConstants.TAGS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<Void> deleteTag(Long id)
	{
		Assert.isTrue(tagRepository.existsById(id), () -> new ResourceNotFoundException("Tag", "id", id));

		// Check if any active posts are still associated with this tag
		Assert.isFalse(postRepository.existsByTagsId(id),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Cannot delete tag as it is still referenced by active posts"));

		tagRepository.deleteById(id);
		log.info("Tag deleted id: {}", id);
		return ApiResponse.success("Tag deleted successfully", null);
	}

	private void validateUniqueConstraints(Tag existing, TagRequest request)
	{
		if (StrUtil.isNotBlank(request.name())
				&& (existing == null || !StrUtil.equals(existing.getName(), request.name())))
		{
			Assert.isFalse(tagRepository.findByNameIncludeDeleted(request.name()).isPresent(),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
							"Tag name already exists: " + request.name()));
		}
		if (StrUtil.isNotBlank(request.slug())
				&& (existing == null || !StrUtil.equals(existing.getSlug(), request.slug())))
		{
			Assert.isFalse(tagRepository.findBySlugIncludeDeleted(request.slug()).isPresent(),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
							"Tag slug already exists: " + request.slug()));
		}
	}
}
