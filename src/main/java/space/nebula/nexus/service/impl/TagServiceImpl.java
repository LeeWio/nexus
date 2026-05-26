package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
	private final TagMapper tagMapper;
	private final RedisUtil redisUtil;

	@Override
	public ApiResponse<List<TagResponse>> getAllTags()
	{
		return ApiResponse.success(tagMapper.toResponseList(tagRepository.findAll()));
	}

	@Override
	@Transactional
	@LogOperation("Create Tag")
	public ApiResponse<TagResponse> createTag(TagRequest request)
	{
		validateUniqueConstraints(null, request);

		Tag tag = new Tag();
		tagMapper.updateEntity(tag, request);

		tagRepository.save(tag);
		log.info("Tag created: {}", tag.getName());
		clearSeoCache();
		return ApiResponse.success("Tag created successfully", tagMapper.toResponse(tag));
	}

	@Override
	@Transactional
	@LogOperation("Update Tag")
	public ApiResponse<TagResponse> updateTag(Long id, TagRequest request)
	{
		Tag existingTag = tagRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

		validateUniqueConstraints(existingTag, request);

		tagMapper.updateEntity(existingTag, request);
		tagRepository.save(existingTag);

		log.info("Tag updated: {}", existingTag.getName());
		clearSeoCache();
		return ApiResponse.success("Tag updated successfully", tagMapper.toResponse(existingTag));
	}

	@Override
	@Transactional
	@LogOperation("Delete Tag")
	public ApiResponse<Void> deleteTag(Long id)
	{
		Assert.isTrue(tagRepository.existsById(id), () -> new ResourceNotFoundException("Tag", "id", id));
		tagRepository.deleteById(id);
		log.info("Tag deleted id: {}", id);
		clearSeoCache();
		return ApiResponse.success("Tag deleted successfully", null);
	}

	private void validateUniqueConstraints(Tag existing, TagRequest request)
	{
		if (StrUtil.isNotBlank(request.name())
				&& (existing == null || !StrUtil.equals(existing.getName(), request.name())))
		{
			Assert.isFalse(tagRepository.findByName(request.name()).isPresent(),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
							"Tag name already exists: " + request.name()));
		}
		if (StrUtil.isNotBlank(request.slug())
				&& (existing == null || !StrUtil.equals(existing.getSlug(), request.slug())))
		{
			Assert.isFalse(tagRepository.findBySlug(request.slug()).isPresent(),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
							"Tag slug already exists: " + request.slug()));
		}
	}

	private void clearSeoCache()
	{
		redisUtil.delete(CacheConstants.buildFullKey(CacheConstants.SEO, CacheConstants.SITEMAP_KEY));
	}
}
