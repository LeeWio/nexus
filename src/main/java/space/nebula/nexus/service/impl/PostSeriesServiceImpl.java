package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
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
import space.nebula.nexus.entity.PostSeries;
import space.nebula.nexus.mapper.PostSeriesMapper;
import space.nebula.nexus.payload.request.SeriesRequest;
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
	public ApiResponse<SeriesResponse> createSeries(SeriesRequest request)
	{
		Assert.isFalse(seriesRepository.existsBySlug(request.slug()), () -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Series slug already exists: " + request.slug()));

		PostSeries series = new PostSeries();
		seriesMapper.updateEntity(series, request);

		PostSeries savedSeries = seriesRepository.save(series);
		log.info("Created new post series: {}", savedSeries.getName());
		clearSeoCache();
		return ApiResponse.success("Series created successfully", seriesMapper.toResponse(savedSeries));
	}

	@Override
	@Transactional
	@LogOperation("Update Post Series")
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
		clearSeoCache();
		return ApiResponse.success("Series updated successfully", seriesMapper.toResponse(updatedSeries));
	}

	@Override
	@Transactional
	@LogOperation("Delete Post Series")
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
		clearSeoCache();
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

	private PostSeries findSeriesOrThrow(Long id)
	{
		return seriesRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Series", "id", id));
	}

	private void clearSeoCache()
	{
		redisUtil.delete(CacheConstants.buildFullKey(CacheConstants.SEO, CacheConstants.SITEMAP_KEY));
	}
}
