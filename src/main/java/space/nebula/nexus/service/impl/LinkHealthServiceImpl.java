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

    @Override
    public void runFullScan() {
        log.info("Starting full external link health check scan...");
        
        AtomicInteger brokenCount = new AtomicInteger(0);
        java.util.List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

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

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            int found = brokenCount.get();
            log.info("Full link scan completed. Found {} broken links.", found);
            if (found > 0) {
                notifyAdmins(found);
            }
        });
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

    private CompletableFuture<Void> checkLinkAsync(String url, String sourceType, Long sourceId, String sourceTitle, AtomicInteger brokenCount) {
        return CompletableFuture.runAsync(() -> {
            try {
                int status = HttpUtil.createGet(url).timeout(5000).execute().getStatus();
                boolean isBroken = status >= 400;
                if (isBroken) brokenCount.incrementAndGet();
                updateLog(url, sourceType, sourceId, sourceTitle, status, isBroken, null);
            } catch (Exception e) {
                brokenCount.incrementAndGet();
                updateLog(url, sourceType, sourceId, sourceTitle, null, true, e.getMessage());
            }
        }, asyncExecutor);
    }

    private void updateLog(String url, String sourceType, Long sourceId, String sourceTitle, Integer status, boolean isBroken, String error) {
        LinkCheckLog logEntry = linkCheckLogRepository.findByUrlAndSourceTypeAndSourceId(url, sourceType, sourceId)
                .orElse(new LinkCheckLog());
        
        logEntry.setUrl(url);
        logEntry.setSourceType(sourceType);
        logEntry.setSourceId(sourceId);
        logEntry.setSourceTitle(sourceTitle);
        logEntry.setStatusCode(status);
        logEntry.setIsBroken(isBroken);
        logEntry.setErrorMessage(StrUtil.maxLength(error, 450));
        
        linkCheckLogRepository.save(logEntry);
        
        if (isBroken) {
            log.warn("Broken link detected in {}: {} -> {}", sourceType, sourceTitle, url);
        }
    }

    private Set<String> extractLinks(String content) {
        if (StrUtil.isBlank(content)) return new HashSet<>();
        List<String> urls = ReUtil.findAll(URL_REGEX, content, 0);
        return new HashSet<>(urls);
    }
}
