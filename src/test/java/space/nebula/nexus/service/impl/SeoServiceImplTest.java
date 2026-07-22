package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import space.nebula.nexus.entity.Config;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.repository.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeoServiceImplTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private PostSeriesRepository seriesRepository;
    @Mock
    private ConfigRepository configRepository;

    @InjectMocks
    private SeoServiceImpl seoService;

    @Test
    void generateSitemapXml_Success() {
        Config config = new Config();
        config.setConfigValue("http://example.com");
        when(configRepository.findByConfigKey("site_url")).thenReturn(Optional.of(config));
        
        Post post = new Post();
        post.setSlug("test-post?a=1&b=2");
        when(postRepository.findAllByStatus(any(), any())).thenReturn(new PageImpl<>(List.of(post)));
        
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(tagRepository.findAll()).thenReturn(Collections.emptyList());
        when(seriesRepository.findByIsPublishedTrueOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();

        assertNotNull(xml);
        assertTrue(xml.contains("<loc>http://example.com/posts/test-post?a=1&amp;b=2</loc>"));
    }

    @Test
    void generateRssFeedXml_UsesPublishedAtAndAutoSummaryFallback() {
        Config url = new Config();
        url.setConfigValue("http://example.com");
        Config name = new Config();
        name.setConfigValue("Example");
        Config description = new Config();
        description.setConfigValue("Description");
        when(configRepository.findByConfigKey("site_name")).thenReturn(Optional.of(name));
        when(configRepository.findByConfigKey("site_description")).thenReturn(Optional.of(description));
        when(configRepository.findByConfigKey("site_url")).thenReturn(Optional.of(url));

        Post post = new Post();
        post.setTitle("RSS Post");
        post.setSlug("rss-post");
        post.setAutoSummary("Generated summary");
        post.setPublishedAt(LocalDateTime.of(2026, 7, 20, 10, 0));
        when(postRepository.findAllByStatus(any(), any())).thenReturn(new PageImpl<>(List.of(post)));

        String xml = seoService.generateRssFeedXml();

        assertTrue(xml.contains("RSS Post"));
        assertTrue(xml.contains("Generated summary"));
    }

    @Test
    void generateRobotsTxt_Success() {
        Config config = new Config();
        config.setConfigValue("http://example.com");
        when(configRepository.findByConfigKey("site_url")).thenReturn(Optional.of(config));

        String txt = seoService.generateRobotsTxt();

        assertNotNull(txt);
        assertTrue(txt.contains("Sitemap: http://example.com/api/v1/public/seo/sitemap.xml"));
    }
}
