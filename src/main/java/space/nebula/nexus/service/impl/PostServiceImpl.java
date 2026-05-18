package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostDeletedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.payload.request.PostAutosaveRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.PostSeriesRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.service.IPostService;
import space.nebula.nexus.utils.RedisUtil;
import space.nebula.nexus.utils.SlugUtil;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * Professional implementation of IPostService with Event-Driven decoupling, MapStruct, and Autosave.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostSeriesRepository seriesRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final RedisUtil redisUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final IInteractionService interactionService;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<PostResponse>> searchPostsForAdmin(Pageable pageable) {
        Page<PostResponse> adminPostPage = postRepository.findAll(pageable).map(postMapper::toResponse);
        return ApiResponse.success(PageResult.of(adminPostPage));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PostResponse> retrievePostById(Long id) {
        Post post = findPostOrThrow(id);
                
        PostResponse.PostResponseBuilder responseBuilder = postMapper.toResponse(post).toBuilder();
        interactionService.populateInteractionData(responseBuilder, id);
        
        return ApiResponse.success(responseBuilder.build());
    }

    @Override
    @Transactional
    @LogOperation("Create Blog Post")
    public ApiResponse<PostResponse> createPost(PostRequest request) {
        String uniqueSlug = validateAndGenerateSlug(request.slug(), request.title());
        User currentAuthor = SecurityUtil.getCurrentUserOrThrow(userRepository);

        Post newPost = new Post();
        postMapper.updateEntity(newPost, request);
        newPost.setSlug(uniqueSlug);
        newPost.setAuthor(currentAuthor);
        
        syncCategoryAndTags(newPost, request);

        postRepository.save(newPost);
        log.info("Blog post created: {} by {}", newPost.getTitle(), currentAuthor.getUsername());
        
        // Cleanup potential autosave data (client might use slug or random UUID as identifier)
        clearAutosaveData(request.slug());

        // Publish event for async side-effects (Search indexing, Revisions)
        eventPublisher.publishEvent(new PostChangedEvent(this, newPost, true));
        
        clearPostListCache();
        
        return ApiResponse.success("Post created successfully", postMapper.toResponse(newPost));
    }

    @Override
    @Transactional
    @LogOperation("Update Blog Post")
    public ApiResponse<PostResponse> updatePost(Long id, PostRequest request) {
        Post existingPost = findPostOrThrow(id);

        String newSlug = existingPost.getSlug();
        if (request.slug() != null && !request.slug().equals(existingPost.getSlug())) {
            newSlug = validateAndGenerateSlug(request.slug(), request.title());
        }

        postMapper.updateEntity(existingPost, request);
        existingPost.setSlug(newSlug);
        
        syncCategoryAndTags(existingPost, request);

        postRepository.save(existingPost);
        log.info("Blog post updated: {}", existingPost.getTitle());
        
        // Cleanup autosave data
        clearAutosaveData(id.toString());
        clearAutosaveData(existingPost.getSlug());

        // Publish event
        eventPublisher.publishEvent(new PostChangedEvent(this, existingPost, false));
        
        clearPostCache(existingPost.getSlug(), newSlug);
        
        return ApiResponse.success("Post updated successfully", postMapper.toResponse(existingPost));
    }

    @Override
    @Transactional
    @LogOperation("Delete Blog Post")
    public ApiResponse<Void> deletePost(Long id) {
        Post postToDelete = findPostOrThrow(id);
        String currentSlug = postToDelete.getSlug();
                
        postRepository.delete(postToDelete);
        log.info("Blog post deleted id: {}", id);
        
        // Publish deletion event
        eventPublisher.publishEvent(new PostDeletedEvent(this, id, currentSlug));
        
        clearPostCache(currentSlug, null);
        
        return ApiResponse.success("Post deleted successfully", null);
    }

    @Override
    @Cacheable(value = CacheConstants.BLOG_POSTS, key = CacheConstants.POST_LIST_KEY)
    public ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword, Pageable pageable) {
        Page<Post> publishedPosts = postRepository.findAll((root, query, cb) -> {
            var searchCriteria = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            searchCriteria.add(cb.equal(root.get("status"), PostStatus.PUBLISHED));
            
            if (categoryId != null) {
                searchCriteria.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (tagId != null) {
                searchCriteria.add(cb.isMember(tagId, root.get("tags")));
            }
            if (keyword != null && !keyword.isBlank()) {
                String keywordPattern = "%" + keyword.toLowerCase() + "%";
                searchCriteria.add(cb.or(
                    cb.like(cb.lower(root.get("title")), keywordPattern),
                    cb.like(cb.lower(root.get("summary")), keywordPattern),
                    cb.like(cb.lower(root.get("content")), keywordPattern)
                ));
            }
            return cb.and(searchCriteria.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
        
        return ApiResponse.success(PageResult.of(publishedPosts.map(postMapper::toResponse)));
    }

    @Override
    public ApiResponse<PostResponse> retrievePostBySlug(String slug) {
        String cacheKey = CacheConstants.POST_SLUG_PREFIX + slug;
        PostResponse postResponse = redisUtil.get(cacheKey, PostResponse.class)
                .orElseGet(() -> {
                    Post post = postRepository.findBySlug(slug)
                            .orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));

                    if (post.getStatus() != PostStatus.PUBLISHED) {
                        throw new BusinessException(403, "Post is not published");
                    }
                    PostResponse mappedResponse = postMapper.toResponse(post);
                    redisUtil.set(cacheKey, mappedResponse, 1, java.util.concurrent.TimeUnit.HOURS);
                    return mappedResponse;
                });

        // Async view count increment
        redisUtil.increment(CacheConstants.POST_VIEW_COUNT + postResponse.id());
        
        // Merge real-time views from Redis hash
        Long currentViews = postResponse.views();
        Object extraViews = redisUtil.hashGet(CacheConstants.POST_VIEW_EXTRA_HASH, postResponse.id().toString());
        if (extraViews instanceof Number extraViewCount) {
            currentViews += extraViewCount.longValue();
        }
        
        PostResponse finalPostResponse = postResponse.toBuilder().views(currentViews).build();
        return ApiResponse.success(finalPostResponse);
    }

    @Override
    public ApiResponse<Void> autosavePostContent(PostAutosaveRequest request) {
        String autosaveKey = CacheConstants.POST_AUTOSAVE_PREFIX + request.identifier();
        redisUtil.set(autosaveKey, request.content(), 24, TimeUnit.HOURS);
        log.debug("Autosaved content for identifier: {}", request.identifier());
        return ApiResponse.success("Content autosaved", null);
    }

    @Override
    public ApiResponse<String> retrieveAutosavedContent(String identifier) {
        String autosaveKey = CacheConstants.POST_AUTOSAVE_PREFIX + identifier;
        return redisUtil.get(autosaveKey, String.class)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "No autosaved content found for this identifier"));
    }

    // --- Private Helper Methods ---

    private void clearAutosaveData(String identifier) {
        if (identifier != null) {
            redisUtil.delete(CacheConstants.POST_AUTOSAVE_PREFIX + identifier);
        }
    }

    private Post findPostOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
    }

    private String validateAndGenerateSlug(String requestedSlug, String title) {
        String slug = (requestedSlug == null || requestedSlug.isBlank()) 
                ? SlugUtil.toSlug(title) 
                : SlugUtil.toSlug(requestedSlug);
        
        if (postRepository.findBySlug(slug).isPresent()) {
            throw new BusinessException("Post slug already exists: " + slug);
        }
        return slug;
    }

    private void syncCategoryAndTags(Post post, PostRequest request) {
        if (request.categoryId() != null) {
            post.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId())));
        } else if (request.categoryId() == null) {
            post.setCategory(null);
        }

        // Sync Series
        if (request.seriesId() != null) {
            post.setSeries(seriesRepository.findById(request.seriesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Series", "id", request.seriesId())));
            post.setSeriesOrder(request.seriesOrder() != null ? request.seriesOrder() : 0);
        } else {
            post.setSeries(null);
            post.setSeriesOrder(0);
        }

        if (request.tagIds() != null) {
            post.setTags(new HashSet<>(tagRepository.findAllById(request.tagIds())));
        }
    }

    private void clearPostListCache() {
        redisUtil.delete(CacheConstants.BLOG_POSTS + "::" + CacheConstants.POST_LIST_KEY);
        redisUtil.delete("nexus:cache:seo::sitemap");
        redisUtil.delete("nexus:cache:seo::rss_feed");
    }

    private void clearPostCache(String oldSlug, String newSlug) {
        clearPostListCache();
        redisUtil.delete(CacheConstants.POST_SLUG_PREFIX + oldSlug);
        if (newSlug != null && !newSlug.equals(oldSlug)) {
            redisUtil.delete(CacheConstants.POST_SLUG_PREFIX + newSlug);
        }
    }
}
