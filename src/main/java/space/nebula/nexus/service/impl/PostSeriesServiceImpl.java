package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostSeries;
import space.nebula.nexus.mapper.PostSeriesMapper;
import space.nebula.nexus.payload.request.SeriesRequest;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.SeriesResponse;
import space.nebula.nexus.repository.PostSeriesRepository;
import space.nebula.nexus.service.IPostSeriesService;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostSeriesServiceImpl implements IPostSeriesService
{

	private final PostSeriesRepository seriesRepository;
	private final PostSeriesMapper seriesMapper;
	private final space.nebula.nexus.mapper.PostMapper postMapper;
	private final RedisUtil redisUtil;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<SeriesResponse>> retrieveAllSeriesForAdmin()
	{
		return ApiResponse.success(seriesMapper.toResponseList(seriesRepository.findAll()));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<SeriesResponse> retrieveSeriesById(Long id)
	{
		PostSeries series = findSeriesOrThrow(id);
		return ApiResponse.success(seriesMapper.toResponse(series));
	}

	@Override
	@Transactional
	@LogOperation("Create Post Series")
	@CacheEvict(value = { CacheConstants.PROJECTS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<SeriesResponse> createSeries(SeriesRequest request)
	{
		Assert.isFalse(seriesRepository.existsBySlug(request.slug()), () -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Series slug already exists: " + request.slug()));

		PostSeries series = new PostSeries();
		seriesMapper.updateEntity(series, request);

		PostSeries savedSeries = seriesRepository.save(series);
		log.info("Created new post series: {}", savedSeries.getName());
		return ApiResponse.success("Series created successfully", seriesMapper.toResponse(savedSeries));
	}

	@Override
	@Transactional
	@LogOperation("Update Post Series")
	@CacheEvict(value = { CacheConstants.PROJECTS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<SeriesResponse> updateSeries(Long id, SeriesRequest request)
	{
		PostSeries series = findSeriesOrThrow(id);

		if (request.slug() != null && !request.slug().equals(series.getSlug()))
		{
			Assert.isFalse(seriesRepository.existsBySlug(request.slug()), () -> new BusinessException(BusinessCode.DUPLICATE_KEY,
					"Series slug already exists: " + request.slug()));
		}

		seriesMapper.updateEntity(series, request);
		PostSeries updatedSeries = seriesRepository.save(series);
		log.info("Updated post series: {}", updatedSeries.getName());
		return ApiResponse.success("Series updated successfully", seriesMapper.toResponse(updatedSeries));
	}

	@Override
	@Transactional
	@LogOperation("Delete Post Series")
	@CacheEvict(value = { CacheConstants.PROJECTS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<Void> deleteSeries(Long id)
	{
		PostSeries series = findSeriesOrThrow(id);

		// Unlink posts from this series
		series.getPosts().forEach(post ->
		{
			post.setSeries(null);
			post.setSeriesOrder(0);
		});

		seriesRepository.delete(series);
		log.info("Deleted post series ID: {}", id);
		return ApiResponse.success("Series deleted successfully", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<SeriesResponse>> retrievePublicSeriesList()
	{
		List<PostSeries> publicSeries = seriesRepository.findByIsPublishedTrueOrderByCreatedAtDesc();
		return ApiResponse.success(seriesMapper.toResponseList(publicSeries));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<SeriesResponse> retrieveSeriesWithPosts(String slug)
	{
		PostSeries series = seriesRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Series", "slug", slug));

		Assert.isTrue(series.getIsPublished(), () -> new BusinessException(BusinessCode.FORBIDDEN, "This series is not publicly available"));

		return ApiResponse.success(seriesMapper.toResponseWithPosts(series));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<cn.hutool.core.lang.tree.Tree<Long>>> retrieveSeriesTree(String slug)
	{
		PostSeries series = seriesRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Series", "slug", slug));

		List<Post> posts = series.getPosts();
		List<PostResponse> postResponses = postMapper.toResponseList(posts);

		cn.hutool.core.lang.tree.TreeNodeConfig config = new cn.hutool.core.lang.tree.TreeNodeConfig();
		config.setIdKey("id");
		config.setParentIdKey("parentId");
		config.setWeightKey("seriesOrder");

		List<cn.hutool.core.lang.tree.Tree<Long>> tree = cn.hutool.core.lang.tree.TreeUtil.build(postResponses, null,
				config, (postResponse, treeNode) ->
				{
					treeNode.setId(postResponse.id());
					treeNode.setParentId(postResponse.parentId());
					treeNode.setWeight(postResponse.seriesOrder());
					treeNode.putExtra("title", postResponse.title());
					treeNode.putExtra("slug", postResponse.slug());
					treeNode.putExtra("path", postResponse.path());
				});

		return ApiResponse.success(tree);
	}

	private PostSeries findSeriesOrThrow(Long id)
	{
		return seriesRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Series", "id", id));
	}
}
