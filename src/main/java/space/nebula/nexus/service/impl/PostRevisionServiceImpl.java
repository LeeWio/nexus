package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostRevision;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.mapper.PostRevisionMapper;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostRevisionResponse;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.PostRevisionRepository;
import space.nebula.nexus.service.IPostRevisionService;
import space.nebula.nexus.service.IPostSearchService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostRevisionServiceImpl implements IPostRevisionService {

    private final PostRevisionRepository postRevisionRepository;
    private final PostRevisionMapper postRevisionMapper;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final IPostSearchService postSearchService;

    @Override
    @Transactional
    public void saveRevision(Post post) {
        int nextVersion = postRevisionRepository.findMaxVersionByPostId(post.getId()).orElse(0) + 1;
        
        PostRevision revision = new PostRevision();
        revision.setPost(post);
        revision.setTitle(post.getTitle());
        revision.setSummary(post.getSummary());
        revision.setContent(post.getContent());
        revision.setVersionNumber(nextVersion);
        revision.setCreatedBy(post.getAuthor()); // In a real scenario, use currently logged in user

        postRevisionRepository.save(revision);
        log.info("Saved revision {} for post {}", nextVersion, post.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PostRevisionResponse>> getPostRevisions(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(404, "Post not found");
        }
        List<PostRevision> revisions = postRevisionRepository.findByPostIdOrderByVersionNumberDesc(postId);
        return ApiResponse.success(postRevisionMapper.toResponseList(revisions));
    }

    @Override
    @Transactional
    public ApiResponse<PostResponse> revertToRevision(Long postId, Long revisionId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "Post not found"));
                
        PostRevision revision = postRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new BusinessException(404, "Revision not found"));
                
        if (!revision.getPost().getId().equals(postId)) {
            throw new BusinessException(400, "Revision does not belong to this post");
        }
        
        // Revert fields
        post.setTitle(revision.getTitle());
        post.setSummary(revision.getSummary());
        post.setContent(revision.getContent());
        
        // Save post
        postRepository.save(post);
        
        // Save a new revision of this reversion
        saveRevision(post);
        
        // Update Search Index
        postSearchService.indexPost(post);
        
        log.info("Reverted post {} to revision {}", postId, revisionId);
        
        return ApiResponse.success("Post reverted to revision " + revision.getVersionNumber(), postMapper.toResponse(post));
    }
}
