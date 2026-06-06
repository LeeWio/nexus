package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.repository.PostRepository;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticGenerationServiceImplTest {

    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private StaticGenerationServiceImpl staticGenerationService;

    @Test
    void generatePostStaticHtml_Success() {
        Post post = new Post();
        post.setSlug("test-post");
        
        when(templateEngine.process(eq("post-static"), any(Context.class))).thenReturn("<html></html>");

        staticGenerationService.generatePostStaticHtml(post);

        verify(storageProvider).store(any(InputStream.class), eq("static/posts/test-post.html"));
    }

    @Test
    void deletePostStaticHtml_Success() {
        staticGenerationService.deletePostStaticHtml("test-post");
        verify(storageProvider).delete(eq("static/posts/test-post.html"));
    }
}
