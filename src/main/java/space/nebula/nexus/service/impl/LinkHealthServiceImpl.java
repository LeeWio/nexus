package space.nebula.nexus.service.impl;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.config.LinkHealthProperties;
import space.nebula.nexus.entity.FriendLink;
import space.nebula.nexus.entity.LinkCheckLog;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.FriendLinkRepository;
import space.nebula.nexus.repository.LinkCheckLogRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.ILinkHealthService;
import space.nebula.nexus.service.INotificationService;
import space.nebula.nexus.security.OutboundUrlValidator;

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
	private final LinkHealthLogPersistenceService linkHealthLogPersistenceService;
	private final INotificationService notificationService;
	private final UserRepository userRepository;
	private final Executor outboundExecutor;
	private final OutboundUrlValidator outboundUrlValidator;
	private final LinkHealthProperties linkHealthProperties;

	// Basic regex to find URLs in Markdown: [text](url) or directly http://...
	private static final String URL_REGEX = "https?://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(?:/[\\w\\.\\-\\?%&=\\+\\!]*)?";

	@Override
	public void runFullScan() {
		log.info("Starting full external link health check scan...");

		AtomicInteger brokenCount = new AtomicInteger(0);
		int pageNumber = 0;
		org.springframework.data.domain.Page<FriendLink> friendPage;
		do {
			friendPage = friendLinkRepository
					.findAll(PageRequest.of(pageNumber++, linkHealthProperties.getFriendPageSize()));
			processBatch(friendPage.getContent().stream()
					.map(link -> new LinkTarget(link.getUrl(), "FRIEND_LINK", link.getId(), link.getName())).toList(),
					brokenCount);
		} while (friendPage.hasNext());

		pageNumber = 0;
		org.springframework.data.domain.Page<Post> postPage;
		do {
			postPage = postRepository.findScanPageByStatus(PostStatus.PUBLISHED,
					PageRequest.of(pageNumber++, linkHealthProperties.getPostPageSize()));
			List<LinkTarget> targets = postPage.getContent().stream().flatMap(post -> extractLinks(post.getContent())
					.stream().map(url -> new LinkTarget(url, "POST", post.getId(), post.getTitle()))).toList();
			processBatch(targets, brokenCount);
		} while (postPage.hasNext());

		int found = brokenCount.get();
		log.info("Full link scan completed. Found {} broken links.", found);
		if (found > 0)
			notifyAdmins(found);
	}

	private record LinkTarget(String url, String sourceType, Long sourceId, String sourceTitle) {
	}

	private void processBatch(List<LinkTarget> targets, AtomicInteger brokenCount) {
		if (targets.isEmpty())
			return;
		List<CompletableFuture<LinkHealthLogPersistenceService.LinkCheckLogUpdate>> futures = targets.stream()
				.map(target -> checkLinkAsync(target.url(), target.sourceType(), target.sourceId(),
						target.sourceTitle(), brokenCount))
				.toList();
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		linkHealthLogPersistenceService.saveBatch(futures.stream().map(CompletableFuture::join).toList());
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
				.forEach(admin -> notificationService.send(admin, "Broken Links Detected", String.format(
						"The latest link health check found %d broken links in your content. Please review them in the admin panel.",
						brokenCount), "HEALTH_CHECK", "/admin/content/links"));
	}

	private CompletableFuture<LinkHealthLogPersistenceService.LinkCheckLogUpdate> checkLinkAsync(String url,
			String sourceType, Long sourceId, String sourceTitle, AtomicInteger brokenCount) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				outboundUrlValidator.validate(url);
				// 1. Try HEAD first (fast, minimal bandwidth)
				int status;
				try (HttpResponse response = HttpUtil.createRequest(cn.hutool.http.Method.HEAD, url)
						.setFollowRedirects(false).timeout(requestTimeoutMillis()).execute()) {
					status = response.getStatus();
				}
				if (status == 405 || status == 501) {
					status = executeGet(url);
				}
				boolean isBroken = status >= 400;
				if (isBroken)
					brokenCount.incrementAndGet();
				return new LinkHealthLogPersistenceService.LinkCheckLogUpdate(url, sourceType, sourceId, sourceTitle,
						status, isBroken, null);
			} catch (Exception e) {
				try {
					// 2. Fall back to GET on any exception
					outboundUrlValidator.validate(url);
					int status = executeGet(url);
					boolean isBroken = status >= 400;
					if (isBroken)
						brokenCount.incrementAndGet();
					return new LinkHealthLogPersistenceService.LinkCheckLogUpdate(url, sourceType, sourceId,
							sourceTitle, status, isBroken, null);
				} catch (Exception ex) {
					brokenCount.incrementAndGet();
					return new LinkHealthLogPersistenceService.LinkCheckLogUpdate(url, sourceType, sourceId,
							sourceTitle, null, true, ex.getMessage());
				}
			}
		}, outboundExecutor);
	}

	private int executeGet(String url) {
		try (HttpResponse response = HttpUtil.createGet(url).setFollowRedirects(false).timeout(requestTimeoutMillis())
				.execute()) {
			return response.getStatus();
		}
	}

	private int requestTimeoutMillis() {
		return Math.toIntExact(linkHealthProperties.getRequestTimeout().toMillis());
	}

	private Set<String> extractLinks(String content) {
		if (StrUtil.isBlank(content))
			return new HashSet<>();
		List<String> urls = ReUtil.findAll(URL_REGEX, content, 0);
		return new HashSet<>(urls);
	}
}
