package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.service.IStaticGenerationService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticGenerationServiceImpl implements IStaticGenerationService
{

	private final TemplateEngine templateEngine;
	private final StorageProvider storageProvider;
	private final space.nebula.nexus.repository.PostRepository postRepository;

	@Async("asyncExecutor")
	@Override
	public void generatePostStaticHtml(Post post)
	{
		log.info("Generating static HTML for post: {}", post.getSlug());
		try
		{
			Context context = new Context();
			context.setVariable("post", post);

			String html = templateEngine.process("post-static", context);
			byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

			String fileName = "static/posts/" + post.getSlug() + ".html";
			storageProvider.store(new ByteArrayInputStream(htmlBytes), fileName);

			log.info("Successfully generated and uploaded static HTML: {}", fileName);
		}
		catch (Exception e)
		{
			log.error("Failed to generate static HTML for post: {}", post.getSlug(), e);
		}
	}

	@Async("asyncExecutor")
	@Override
	public void deletePostStaticHtml(String slug)
	{
		String fileName = "static/posts/" + slug + ".html";
		storageProvider.delete(fileName);
		log.info("Deleted static HTML for post: {}", slug);
	}

	@Override
	public void regenerateAllPosts()
	{
		log.info("Starting site-wide static HTML regeneration...");
		var publishedPosts = postRepository.findAllByStatus(space.nebula.nexus.enums.PostStatus.PUBLISHED,
				org.springframework.data.domain.Pageable.unpaged());

		publishedPosts.getContent().forEach(this::generatePostStaticHtml);
		log.info("Site-wide static HTML regeneration task dispatched for {} posts.",
				publishedPosts.getTotalElements());
	}
}
