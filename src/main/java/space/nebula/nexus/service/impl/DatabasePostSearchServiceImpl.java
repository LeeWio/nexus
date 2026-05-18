package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.entity.document.PostDocument;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.service.IPostSearchService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.type", havingValue = "database")
public class DatabasePostSearchServiceImpl implements IPostSearchService {

    private final PostRepository postRepository;

    @Override
    public void indexPost(Post post) {
        log.debug("Database search mode enabled, skipping indexing for post: {}", post.getId());
    }

    @Override
    public void deletePostIndex(Long postId) {
        log.debug("Database search mode enabled, skipping index deletion for post: {}", postId);
    }

    @Override
    public void rebuildIndex() {
        log.debug("Database search mode enabled, skipping index rebuild.");
    }

    @Override
    public ApiResponse<PageResult<PostDocument>> searchPosts(String keyword, Pageable pageable) {
        Specification<Post> spec = (root, query, cb) -> {
            Specification<Post> statusSpec = (r, q, c) -> c.equal(r.get("status"), PostStatus.PUBLISHED);
            
            if (keyword == null || keyword.isBlank()) {
                return statusSpec.toPredicate(root, query, cb);
            }

            String pattern = "%" + keyword.toLowerCase() + "%";
            Specification<Post> keywordSpec = (r, q, c) -> c.or(
                    c.like(c.lower(r.get("title")), pattern),
                    c.like(c.lower(r.get("summary")), pattern),
                    c.like(c.lower(r.get("content")), pattern)
            );
            
            return Specification.where(statusSpec).and(keywordSpec).toPredicate(root, query, cb);
        };

        Page<Post> posts = postRepository.findAll(spec, pageable);
        Page<PostDocument> documents = posts.map(this::mapToDocument);
        
        return ApiResponse.success(PageResult.of(documents));
    }

    private PostDocument mapToDocument(Post post) {
        List<String> tagNames = post.getTags() != null 
                ? post.getTags().stream().map(Tag::getName).collect(Collectors.toList())
                : List.of();

        return PostDocument.builder()
                .id(post.getId().toString())
                .title(post.getTitle())
                .slug(post.getSlug())
                .summary(post.getSummary())
                .content(post.getContent())
                .authorName(post.getAuthor() != null ? post.getAuthor().getNickname() : null)
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .tags(tagNames)
                .publishedAt(post.getPublishedAt())
                .views(post.getViews())
                .build();
    }
}
