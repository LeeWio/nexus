package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.service.ISeoService;

@Tag(name = "SEO & Feeds", description = "Endpoints for Search Engine Optimization and Subscriptions")
@RestController
@RequestMapping("/api/v1/public/seo")
@RequiredArgsConstructor
public class PublicSeoController {

    private final ISeoService seoService;

    @Operation(summary = "Get sitemap.xml", description = "Returns the site structure in standard XML format for search engines")
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemap() {
        return seoService.generateSitemap();
    }

    @Operation(summary = "Get RSS Feed", description = "Returns the latest posts in RSS 2.0 format for subscribers")
    @GetMapping(value = "/feed.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getRssFeed() {
        return seoService.generateRss();
    }
}
