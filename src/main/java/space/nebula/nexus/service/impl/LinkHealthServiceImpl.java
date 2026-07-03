package space.nebula.nexus.service.impl;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.entity.LinkCheckLog;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.FriendLinkRepository;
import space.nebula.nexus.repository.LinkCheckLogRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.ILinkHealthService;
import space.nebula.nexus.service.INotificationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkHealthServiceImpl implements ILinkHealthService {

    private final PostRepository postRepository;
    private final FriendLinkRepository friendLinkRepository;
    private final LinkCheckLogRepository linkCheckLogRepository;
    private final INotificationService notificationService;
    private final UserRepository userRepository;
    private final Executor asyncExecutor;

    // Basic regex to find URLs in Markdown: [text](url) or directly http://...
    private static final String URL_REGEX = "https?://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(?:/[\\w\\.\\-\\?%&=\\+\\!]*)?";

    private record LinkCheckResult(
            String url,
            String sourceType,
            Long sourceId,
            String sourceTitle,
            Integer status,
            boolean isBroken,
            String error
    ) {}

    @Override
    public void runFullScan() {
        log.info("Starting full external link health check scan...");
        
        AtomicInteger brokenCount = new AtomicInteger(0);
        java.util.List<CompletableFuture<LinkCheckResult>> futures = new java.util.ArrayList<>();

        // 1. Check Friend Links
        List<FriendLink> friendLinks = friendLinkRepository.findAll();
        friendLinks.forEach(fl -> futures.add(checkLinkAsync(fl.getUrl(), "FRIEND_LINK", fl.getId(), fl.getName(), brokenCount)));

        // 2. Check Post Content
        List<Post> posts = postRepository.findAll();
        posts.stream().filter(Post::isPublished).forEach(post -> {
            Set<String> urls = extractLinks(post.getContent());
            urls.forEach(url -> futures.add(checkLinkAsync(url, "POST", post.getId(), post.getTitle(), brokenCount)));
        });

        if (futures.isEmpty()) {
            log.info("No links found to scan.");
            return;
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
            List<LinkCheckResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(java.util.stream.Collectors.toList());

            // Save results sequentially using one database connection to avoid pool starvation
            saveResultsInBatch(results);

            int found = brokenCount.get();
            log.info("Full link scan completed. Found {} broken links.", found);
            if (found > 0) {
                notifyAdmins(found);
            }
        }).join(); // Sync wait on the scheduling thread for accurate metrics/logging
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PageResult<LinkCheckLog>> getBrokenLinks(Pageable pageable) {
        return ApiResponse.success(PageResult.of(linkCheckLogRepository.findByIsBrokenTrue(pageable)));
    }

    @Override
    @Transactional
    public ApiResponse<Void> clearLogs() {
        linkCheckLogRepository.deleteAll();
        return ApiResponse.success("Health check logs cleared", null);
    }

    private void notifyAdmins(int brokenCount) {
        userRepository.findAll().stream()
            .filter(u -> u.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getCode())))
            .forEach(admin -> notificationService.send(
                admin,
                "Broken Links Detected",
                String.format("The latest link health check found %d broken links in your content. Please review them in the admin panel.", brokenCount),
                "HEALTH_CHECK",
                "/admin/content/links"
            ));
    }

    private CompletableFuture<LinkCheckResult> checkLinkAsync(String url, String sourceType, Long sourceId, String sourceTitle, AtomicInteger brokenCount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Try HEAD first (fast, minimal bandwidth)
                int status = HttpUtil.createRequest(cn.hutool.http.Method.HEAD, url).timeout(5000).execute().getStatus();
                if (status == 405 || status == 501) {
                    status = HttpUtil.createGet(url).timeout(5000).execute().getStatus();
                }
                boolean isBroken = status >= 400;
                if (isBroken) brokenCount.incrementAndGet();
                return new LinkCheckResult(url, sourceType, sourceId, sourceTitle, status, isBroken, null);
            } catch (Exception e) {
                try {
                    // 2. Fall back to GET on any exception
                    int status = HttpUtil.createGet(url).timeout(5000).execute().getStatus();
                    boolean isBroken = status >= 400;
                    if (isBroken) brokenCount.incrementAndGet();
                    return new LinkCheckResult(url, sourceType, sourceId, sourceTitle, status, isBroken, null);
                } catch (Exception ex) {
                    brokenCount.incrementAndGet();
                    return new LinkCheckResult(url, sourceType, sourceId, sourceTitle, null, true, ex.getMessage());
                }
            }
        }, asyncExecutor);
    }

    private void saveResultsInBatch(List<LinkCheckResult> results) {
        log.info("Persisting {} link health check logs...", results.size());
        for (LinkCheckResult result : results) {
            try {
                LinkCheckLog logEntry = linkCheckLogRepository.findByUrlAndSourceTypeAndSourceId(result.url(), result.sourceType(), result.sourceId())
                        .orElse(new LinkCheckLog());
                
                logEntry.setUrl(result.url());
                logEntry.setSourceType(result.sourceType());
                logEntry.setSourceId(result.sourceId());
                logEntry.setSourceTitle(result.sourceTitle());
                logEntry.setStatusCode(result.status());
                logEntry.setIsBroken(result.isBroken());
                logEntry.setErrorMessage(StrUtil.maxLength(result.error(), 450));
                
                linkCheckLogRepository.save(logEntry);
                
                if (result.isBroken()) {
                    log.warn("Broken link detected in {}: {} -> {}", result.sourceType(), result.sourceTitle(), result.url());
                }
            } catch (Exception e) {
                log.error("Failed to save health check log for url: {}", result.url(), e);
            }
        }
    }

    private Set<String> extractLinks(String content) {
        if (StrUtil.isBlank(content)) return new HashSet<>();
        List<String> urls = ReUtil.findAll(URL_REGEX, content, 0);
        return new HashSet<>(urls);
    }
}
