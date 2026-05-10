package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;

public interface ISeoService {
    
    /**
     * Generate sitemap.xml content.
     */
    String generateSitemap();

    /**
     * Generate RSS feed content.
     */
    String generateRss();
}
