package space.nebula.nexus.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IPostService;
import space.nebula.nexus.utils.RedisUtil;
import space.nebula.nexus.utils.SlugUtil;

import java.util.HashSet;
import java.util.Optional;

/**
 * Implementation of IPostService with modern refactoring and Redis optimization.
 */
@Slf4j
@Service
public class PostServiceImpl implements IPostService {

    @Resource
    private PostRepository postRepository;

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private TagRepository tagRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private PostMapper postMapper;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public ApiResponse<PageResult<PostResponse>> getAdminPosts(Pageable pageable) {
        Page<PostResponse> page = postRepository.findAll(pageable).map(postMapper::toResponse);
        return ApiResponse.success(PageResult.of(page));
    }

    @Override
    public ApiResponse<PostResponse> getPostById(Long id) {
        return postRepository.findById(id)
                .map(post -> ApiResponse.success(postMapper.toResponse(post)))
                .orElseThrow(() -> new BusinessException(404, "Post not found"));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConstants.BLOG_POSTS, allEntries = true)
    @LogOperation("Create Blog Post")
    public ApiResponse<PostResponse> createPost(PostRequest request) {
        String slug = validateAndGenerateSlug(request.slug(), request.title());
        User author = getCurrentUserOrThrow();

        Post post = new Post();
        updatePostEntity(post, request, slug, author);

        postRepository.save(post);
        log.info("Blog post created: {} by {}", post.getTitle(), author.getUsername());
        return ApiResponse.success("Post created successfully", postMapper.toResponse(post));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConstants.BLOG_POSTS, allEntries = true)
    @LogOperation("Update Blog Post")
    public ApiResponse<PostResponse> updatePost(Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Post not found"));

        String newSlug = post.getSlug();
        if (request.slug() != null && !request.slug().equals(post.getSlug())) {
            newSlug = validateAndGenerateSlug(request.slug(), request.title());
        }

        updatePostEntity(post, request, newSlug, post.getAuthor());

        postRepository.save(post);
        log.info("Blog post updated: {}", post.getTitle());
        return ApiResponse.success("Post updated successfully", postMapper.toResponse(post));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConstants.BLOG_POSTS, allEntries = true)
    @LogOperation("Delete Blog Post")
    public ApiResponse<Void> deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new BusinessException(404, "Post not found");
        }
        postRepository.deleteById(id);
        log.info("Blog post deleted id: {}", id);
        return ApiResponse.success("Post deleted successfully", null);
    }

    @Override
    @Cacheable(value = CacheConstants.BLOG_POSTS, key = CacheConstants.POST_LIST_KEY)
    public ApiResponse<PageResult<PostResponse>> getPublishedPosts(Long categoryId, Long tagId, String keyword, Pageable pageable) {
        Page<Post> posts = postRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("status"), PostStatus.PUBLISHED));
            
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (tagId != null) {
                predicates.add(cb.isMember(tagId, root.get("tags")));
            }
            if (keyword != null && !keyword.isBlank()) {
                String lk = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), lk),
                    cb.like(cb.lower(root.get("summary")), lk),
                    cb.like(cb.lower(root.get("content")), lk)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
        
        return ApiResponse.success(PageResult.of(posts.map(postMapper::toResponse)));
    }

    @Override
    public ApiResponse<PostResponse> getPostBySlug(String slug) {
        // 1. Try to get from manual cache to handle view counts accurately
        String cacheKey = "nexus:post:slug:" + slug;
        Optional<PostResponse> cachedPost = redisUtil.get(cacheKey, PostResponse.class);
        
        PostResponse response;
        if (cachedPost.isPresent()) {
            response = cachedPost.get();
        } else {
            // Cache miss: Load from DB
            Post post = postRepository.findBySlug(slug)
                    .orElseThrow(() -> new BusinessException(404, "Post not found"));

            if (post.getStatus() != PostStatus.PUBLISHED) {
                throw new BusinessException(403, "Post is not published");
            }
            response = postMapper.toResponse(post);
            // Cache for 1 hour
            redisUtil.set(cacheKey, response, 1, java.util.concurrent.TimeUnit.HOURS);
        }

        // 2. Increment view count in Redis (asynchronous buffer)
        String viewKey = CacheConstants.POST_VIEW_COUNT + response.id();
        redisUtil.increment(viewKey);
        
        // 3. Merge current view count into response
        Long currentViews = response.views();
        Object extraViews = redisUtil.hashGet("nexus:post:views:extra", response.id().toString());
        if (extraViews instanceof Number n) {
            currentViews += n.longValue();
        }
        
        // Note: PostResponse is a record, we can't 'set' views. 
        // We need to reconstruct it if we want to show real-time views in the detail response.
        PostResponse updatedResponse = PostResponse.builder()
                .id(response.id())
                .title(response.title())
                .slug(response.slug())
                .coverImage(response.coverImage())
                .summary(response.summary())
                .content(response.content())
                .status(response.status())
                .isFeatured(response.isFeatured())
                .views(currentViews) // Real-time merged views
                .authorName(response.authorName())
                .category(response.category())
                .tags(response.tags())
                .createdAt(response.createdAt())
                .updatedAt(response.updatedAt())
                .build();

        return ApiResponse.success(updatedResponse);
    }

    // --- Private Helper Methods ---

    private String validateAndGenerateSlug(String requestedSlug, String title) {
        String slug = (requestedSlug == null || requestedSlug.isBlank()) 
                ? SlugUtil.toSlug(title) 
                : SlugUtil.toSlug(requestedSlug);
        
        if (postRepository.findBySlug(slug).isPresent()) {
            throw new BusinessException("Post slug already exists: " + slug);
        }
        return slug;
    }

    private User getCurrentUserOrThrow() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Current user not found"));
    }

    private void updatePostEntity(Post post, PostRequest request, String slug, User author) {
        post.setTitle(request.title());
        post.setSlug(slug);
        post.setCoverImage(request.coverImage());
        post.setSummary(request.summary());
        post.setContent(request.content());
        post.setStatus(request.status());
        post.setIsFeatured(request.isFeatured());
        post.setAuthor(author);

        if (request.categoryId() != null) {
            post.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException("Category not found")));
        } else {
            post.setCategory(null);
        }

        if (request.tagIds() != null) {
            post.setTags(new HashSet<>(tagRepository.findAllById(request.tagIds())));
        } else if (post.getTags() != null) {
            post.getTags().clear();
        }
    }
}
