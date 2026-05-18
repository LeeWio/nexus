package space.nebula.nexus.service.impl;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostSeries;
import space.nebula.nexus.entity.Tag;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.repository.*;
import space.nebula.nexus.service.ISeoService;

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeoServiceImpl implements ISeoService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostSeriesRepository seriesRepository;
    private final ConfigRepository configRepository;

    private static final DateTimeFormatter SITEMAP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "seo", key = "'sitemap'")
    public String generateSitemapXml() {
        log.info("Generating professional XML Sitemap...");
        String siteBaseUrl = getSiteBaseUrl();
        StringBuilder sitemapBuilder = new StringBuilder();
        sitemapBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemapBuilder.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. Static Home Page
        appendSitemapEntry(sitemapBuilder, siteBaseUrl, "1.0", "daily");

        // 2. Published Posts
        List<Post> publishedPosts = postRepository.findAll().stream()
                .filter(post -> post.getStatus() == PostStatus.PUBLISHED)
                .toList();
        for (Post post : publishedPosts) {
            appendSitemapEntry(sitemapBuilder, siteBaseUrl + "/posts/" + post.getSlug(), "0.8", "weekly");
        }

        // 3. Content Categories
        List<Category> allCategories = categoryRepository.findAll();
        for (Category category : allCategories) {
            appendSitemapEntry(sitemapBuilder, siteBaseUrl + "/categories/" + category.getSlug(), "0.6", "monthly");
        }

        // 4. Post Series / Columns
        List<PostSeries> activeSeries = seriesRepository.findByIsPublishedTrueOrderByCreatedAtDesc();
        for (PostSeries series : activeSeries) {
            appendSitemapEntry(sitemapBuilder, siteBaseUrl + "/series/" + series.getSlug(), "0.7", "weekly");
        }

        // 5. Common Tags
        List<Tag> allTags = tagRepository.findAll();
        for (Tag tag : allTags) {
            appendSitemapEntry(sitemapBuilder, siteBaseUrl + "/tags/" + tag.getSlug(), "0.4", "monthly");
        }

        sitemapBuilder.append("</urlset>");
        return sitemapBuilder.toString();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "seo", key = "'rss_feed'")
    public String generateRssFeedXml() {
        log.info("Generating standard RSS 2.0 Feed...");
        String websiteName = getSiteConfiguration("site_name", "Nexus Professional Blog");
        String websiteDescription = getSiteConfiguration("site_description", "Hardcore software engineering and personal insights.");
        String siteBaseUrl = getSiteBaseUrl();

        SyndFeed syndFeed = new SyndFeedImpl();
        syndFeed.setFeedType("rss_2.0");
        syndFeed.setTitle(websiteName);
        syndFeed.setLink(siteBaseUrl);
        syndFeed.setDescription(websiteDescription);

        // Fetch 20 most recent published posts
        List<Post> recentPosts = postRepository.findAll().stream()
                .filter(post -> post.getStatus() == PostStatus.PUBLISHED)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(20)
                .toList();

        List<SyndEntry> feedEntries = new ArrayList<>();
        for (Post post : recentPosts) {
            SyndEntry feedEntry = new SyndEntryImpl();
            feedEntry.setTitle(post.getTitle());
            feedEntry.setLink(siteBaseUrl + "/posts/" + post.getSlug());
            feedEntry.setPublishedDate(Date.from(post.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
            
            SyndContent entryDescription = new SyndContentImpl();
            entryDescription.setType("text/plain");
            entryDescription.setValue(post.getSummary() != null ? post.getSummary() : "No summary available.");
            feedEntry.setDescription(entryDescription);
            
            feedEntries.add(feedEntry);
        }

        syndFeed.setEntries(feedEntries);

        try {
            StringWriter xmlWriter = new StringWriter();
            SyndFeedOutput feedOutput = new SyndFeedOutput();
            feedOutput.output(syndFeed, xmlWriter);
            return xmlWriter.toString();
        } catch (Exception feedProcessingException) {
            log.error("Critical failure during RSS feed XML generation", feedProcessingException);
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><error>Internal generation failure</error>";
        }
    }

    private void appendSitemapEntry(StringBuilder builder, String location, String priority, String frequency) {
        builder.append("  <url>\n");
        builder.append("    <loc>").append(location).append("</loc>\n");
        builder.append("    <changefreq>").append(frequency).append("</changefreq>\n");
        builder.append("    <priority>").append(priority).append("</priority>\n");
        builder.append("  </url>\n");
    }

    private String getSiteBaseUrl() {
        return getSiteConfiguration("site_url", "http://localhost:3000");
    }

    private String getSiteConfiguration(String configKey, String fallbackValue) {
        return configRepository.findByConfigKey(configKey)
                .map(space.nebula.nexus.entity.Config::getConfigValue)
                .orElse(fallbackValue);
    }
}
