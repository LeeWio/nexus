package space.nebula.nexus.service.impl;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.ConfigRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.service.ISeoService;

import java.io.StringWriter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeoServiceImpl implements ISeoService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ConfigRepository configRepository;

    @Override
    @Transactional(readOnly = true)
    public String generateSitemap() {
        String baseUrl = getBaseUrl();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. Home
        addUrl(xml, baseUrl, "1.0", "daily");

        // 2. Posts
        List<Post> posts = postRepository.findAll().stream()
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                .toList();
        for (Post post : posts) {
            addUrl(xml, baseUrl + "/posts/" + post.getSlug(), "0.8", "weekly");
        }

        // 3. Categories
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            addUrl(xml, baseUrl + "/categories/" + category.getSlug(), "0.6", "weekly");
        }

        // 4. Tags
        List<Tag> tags = tagRepository.findAll();
        for (Tag tag : tags) {
            addUrl(xml, baseUrl + "/tags/" + tag.getSlug(), "0.6", "weekly");
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String generateRss() {
        String siteName = getSiteConfig("site_name", "Nexus Blog");
        String siteDesc = getSiteConfig("site_description", "Personal Website");
        String baseUrl = getBaseUrl();

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle(siteName);
        feed.setLink(baseUrl);
        feed.setDescription(siteDesc);

        List<Post> posts = postRepository.findAll().stream()
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(20)
                .toList();

        List<SyndEntry> entries = new ArrayList<>();
        for (Post post : posts) {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(post.getTitle());
            entry.setLink(baseUrl + "/posts/" + post.getSlug());
            entry.setPublishedDate(Date.from(post.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
            
            SyndContent description = new SyndContentImpl();
            description.setType("text/plain");
            description.setValue(post.getSummary());
            entry.setDescription(description);
            
            entries.add(entry);
        }

        feed.setEntries(entries);

        try {
            StringWriter writer = new StringWriter();
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("Failed to generate RSS feed", e);
            return "";
        }
    }

    private void addUrl(StringBuilder xml, String url, String priority, String changefreq) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(url).append("</loc>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String getBaseUrl() {
        return getSiteConfig("site_url", "http://localhost:3000");
    }

    private String getSiteConfig(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(space.nebula.nexus.entity.Config::getConfigValue)
                .orElse(defaultValue);
    }
}
