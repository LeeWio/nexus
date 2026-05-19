package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.entity.document.PostDocument;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.IPostSearchService;
import space.nebula.nexus.enums.PostStatus;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.type", havingValue = "elasticsearch", matchIfMissing = true)
public class ElasticsearchPostSearchServiceImpl implements IPostSearchService {

    private final PostSearchRepository postSearchRepository;
    private final PostRepository postRepository;

    @Async("asyncExecutor")
    @Override
    public void indexPost(Post post) {
        if (post.getStatus() != PostStatus.PUBLISHED) {
            deletePostIndex(post.getId());
            return;
        }
        try {
            PostDocument document = mapToDocument(post);
            postSearchRepository.save(document);
            log.info("Successfully indexed post to Elasticsearch: {}", post.getId());
        } catch (Exception e) {
            log.error("Failed to index post to Elasticsearch: {}", post.getId(), e);
        }
    }

    @Async("asyncExecutor")
    @Override
    public void deletePostIndex(Long postId) {
        try {
            postSearchRepository.deleteById(postId.toString());
            log.info("Successfully deleted post from Elasticsearch: {}", postId);
        } catch (Exception e) {
            log.error("Failed to delete post from Elasticsearch: {}", postId, e);
        }
    }

    @Async("asyncExecutor")
    @Override
    public void rebuildIndex() {
        log.info("Starting Elasticsearch index rebuild for posts...");
        postSearchRepository.deleteAll();
        
        int page = 0;
        int size = 100;
        long totalIndexed = 0;
        
        org.springframework.data.domain.Page<Post> postPage;
        do {
            postPage = postRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size));
            List<PostDocument> documents = postPage.getContent().stream()
                    .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                    .map(this::mapToDocument)
                    .toList();
            
            if (!documents.isEmpty()) {
                postSearchRepository.saveAll(documents);
                totalIndexed += documents.size();
            }
            page++;
        } while (postPage.hasNext());

        log.info("Finished rebuilding Elasticsearch index. Total posts indexed: {}", totalIndexed);
    }

    @Override
    public ApiResponse<PageResult<PostDocument>> searchPosts(String keyword, Pageable pageable) {
        Page<PostDocument> page;
        if (keyword == null || keyword.isBlank()) {
            page = postSearchRepository.findAll(pageable);
        } else {
            page = postSearchRepository.findByTitleOrSummaryOrContent(keyword, keyword, keyword, pageable);
        }
        return ApiResponse.success(PageResult.of(page));
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
