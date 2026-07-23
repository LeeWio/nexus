package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.SeriesRequest;
import space.nebula.nexus.payload.response.SeriesResponse;

import java.util.List;

public interface IPostSeriesService {

	ApiResponse<List<SeriesResponse>> retrieveAllSeriesForAdmin();

	ApiResponse<SeriesResponse> retrieveSeriesById(Long id);

	ApiResponse<SeriesResponse> createSeries(SeriesRequest request);

	ApiResponse<SeriesResponse> updateSeries(Long id, SeriesRequest request);

	ApiResponse<Void> deleteSeries(Long id);

	/**
	 * Retrieves the hierarchical tree of posts within a series.
	 */
	ApiResponse<java.util.List<cn.hutool.core.lang.tree.Tree<Long>>> retrieveSeriesTree(String slug);

	ApiResponse<List<SeriesResponse>> retrievePublicSeriesList();

	ApiResponse<SeriesResponse> retrieveSeriesWithPosts(String slug);
}
