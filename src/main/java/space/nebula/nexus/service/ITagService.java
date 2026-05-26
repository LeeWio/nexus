package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.TagRequest;
import space.nebula.nexus.payload.response.TagResponse;

import java.util.List;

public interface ITagService
{
	ApiResponse<List<TagResponse>> getAllTags();

	ApiResponse<TagResponse> createTag(TagRequest request);

	ApiResponse<TagResponse> updateTag(Long id, TagRequest request);

	ApiResponse<Void> deleteTag(Long id);
}
