package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.service.ISeoService;

@Tag(name = "Public SEO & Feeds", description = "Public endpoints for search engine optimization and content subscriptions")
@RestController
@RequestMapping("/api/v1/public/seo")
@RequiredArgsConstructor
public class PublicSeoController {

    private final ISeoService seoService;

    @Operation(summary = "Retrieve sitemap.xml", description = "Returns the hierarchical site structure in standard XML format for crawlers")
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String retrieveSitemap() {
        return seoService.generateSitemapXml();
    }

    @Operation(summary = "Retrieve RSS 2.0 Feed", description = "Returns an XML feed of the most recent published posts for RSS readers")
    @GetMapping(value = "/feed.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String retrieveRssFeed() {
        return seoService.generateRssFeedXml();
    }
}
