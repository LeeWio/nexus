package space.nebula.nexus.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostDeletedEvent;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.service.IStaticGenerationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaticGenerationListener
{

	private final IStaticGenerationService staticGenerationService;

	@Async("asyncExecutor")
	@EventListener
	public void handlePostChanged(PostChangedEvent event)
	{
		var post = event.getPost();
		if (post.getStatus() == PostStatus.PUBLISHED)
		{
			staticGenerationService.generatePostStaticHtml(post);
		}
		else
		{
			// If it was published and now it's not, delete the static file
			staticGenerationService.deletePostStaticHtml(post.getSlug());
		}
	}

	@Async("asyncExecutor")
	@EventListener
	public void handlePostDeleted(PostDeletedEvent event)
	{
		staticGenerationService.deletePostStaticHtml(event.getSlug());
	}
}
