package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
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
public class TagServiceImpl implements ITagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final RedisUtil redisUtil;

    @Override
    public ApiResponse<List<TagResponse>> getAllTags() {
        return ApiResponse.success(tagMapper.toResponseList(tagRepository.findAll()));
    }

    @Override
    @Transactional
    @LogOperation("Create Tag")
    public ApiResponse<TagResponse> createTag(TagRequest request) {
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
    public ApiResponse<TagResponse> updateTag(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

        validateUniqueConstraints(tag, request);
        tagMapper.updateEntity(tag, request);

        tagRepository.save(tag);
        log.info("Tag updated: {}", tag.getName());
        clearSeoCache();
        return ApiResponse.success("Tag updated successfully", tagMapper.toResponse(tag));
    }

    @Override
    @Transactional
    @LogOperation("Delete Tag")
    public ApiResponse<Void> deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag", "id", id);
        }
        tagRepository.deleteById(id);
        log.info("Tag deleted id: {}", id);
        clearSeoCache();
        return ApiResponse.success("Tag deleted successfully", null);
    }

    private void validateUniqueConstraints(Tag existing, TagRequest request) {
        if (request.name() != null && (existing == null || !existing.getName().equals(request.name()))) {
            if (tagRepository.findByName(request.name()).isPresent()) {
                throw new BusinessException("Tag name already exists: " + request.name());
            }
        }
        if (request.slug() != null && (existing == null || !existing.getSlug().equals(request.slug()))) {
            if (tagRepository.findBySlug(request.slug()).isPresent()) {
                throw new BusinessException("Tag slug already exists: " + request.slug());
            }
        }
    }

    private void clearSeoCache() {
        redisUtil.delete("nexus:cache:seo::sitemap");
    }
}
