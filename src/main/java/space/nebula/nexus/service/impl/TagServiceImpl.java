package space.nebula.nexus.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.mapper.TagMapper;
import space.nebula.nexus.payload.request.TagRequest;
import space.nebula.nexus.payload.response.TagResponse;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.service.ITagService;

import java.util.List;

@Slf4j
@Service
public class TagServiceImpl implements ITagService {

    @Resource
    private TagRepository tagRepository;

    @Resource
    private TagMapper tagMapper;

    @Override
    public ApiResponse<List<TagResponse>> getAllTags() {
        return ApiResponse.success(tagMapper.toResponseList(tagRepository.findAll()));
    }

    @Override
    @Transactional
    @LogOperation("Create Tag")
    public ApiResponse<TagResponse> createTag(TagRequest request) {
        if (tagRepository.findByName(request.name()).isPresent()) {
            throw new BusinessException("Tag name already exists");
        }
        if (tagRepository.findBySlug(request.slug()).isPresent()) {
            throw new BusinessException("Tag slug already exists");
        }

        Tag tag = new Tag();
        tag.setName(request.name());
        tag.setSlug(request.slug());

        tagRepository.save(tag);
        log.info("Tag created: {}", tag.getName());
        return ApiResponse.success("Tag created successfully", tagMapper.toResponse(tag));
    }

    @Override
    @Transactional
    @LogOperation("Update Tag")
    public ApiResponse<TagResponse> updateTag(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Tag not found"));

        if (!tag.getName().equals(request.name()) && tagRepository.findByName(request.name()).isPresent()) {
            throw new BusinessException("Tag name already exists");
        }
        if (!tag.getSlug().equals(request.slug()) && tagRepository.findBySlug(request.slug()).isPresent()) {
            throw new BusinessException("Tag slug already exists");
        }

        tag.setName(request.name());
        tag.setSlug(request.slug());

        tagRepository.save(tag);
        log.info("Tag updated: {}", tag.getName());
        return ApiResponse.success("Tag updated successfully", tagMapper.toResponse(tag));
    }

    @Override
    @Transactional
    @LogOperation("Delete Tag")
    public ApiResponse<Void> deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new BusinessException(404, "Tag not found");
        }
        tagRepository.deleteById(id);
        log.info("Tag deleted id: {}", id);
        return ApiResponse.success("Tag deleted successfully", null);
    }
}
